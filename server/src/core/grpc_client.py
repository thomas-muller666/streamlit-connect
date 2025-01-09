import random
import time

import grpc

import proto.navigation_pb2_grpc as nav_grpc
import proto.operations_pb2_grpc as op_grpc
import proto.pingpong_pb2 as pingpong_proto
import proto.pingpong_pb2_grpc as pingpong_grpc


class GrpcClient:
    """
    A client for interacting with a Streamlit backend gRPC server.

    :param address: str - The address of the gRPC server.
    :param logger: obj - The logger object for logging messages.
    :param max_retries: int - The maximum number of connection retries (default is 5).
    :param base_delay: float - The base delay for calculating exponential backoff (default is 1.0).
    :param max_delay: float - The maximum delay for calculating exponential backoff (default is 10.0).
    """

    def __init__(self, address, logger, max_retries=5, base_delay=1.0, max_delay=10.0):
        self._address = address
        self._log = logger
        self._max_retries = max_retries
        self._base_delay = base_delay
        self._max_delay = max_delay
        self._channel = None
        self._ping_pong_stub = None
        self._nav_service_stub = None
        self._op_service_stub = None
        self._reconnect()

    def get_navigation(self, request):
        self._log.debug(f"Getting navigation with request:\n{request}")
        return self._attempt_rpc(self._nav_service_stub.GetNavigation, request)

    def get_operations(self, request):
        self._log.debug(f"Getting operations with request:\n{request}")
        return self._attempt_rpc(self._op_service_stub.GetOperations, request)

    def disconnect(self):
        try:
            self._channel.close()
        except Exception as e:
            self._log.warn(f"Error disconnecting: {e}")
        self._ping_pong_stub = None
        self._nav_service_stub = None
        self._op_service_stub = None

    def _attempt_rpc(self, rpc_call, request):
        for i in range(self._max_retries + 1):
            try:
                if not self._channel:
                    self._reconnect()
                response = rpc_call(request)
                return response
            except grpc.RpcError as e:
                self._log.warn(f"Error calling RPC on attempt '{i}' : {e}")
                self._channel = None
                continue

        raise Exception(f"Failed to get response after {self._max_retries} retries")

    def _check_connection(self):
        try:
            ping_request = pingpong_proto.PingRequest()
            self._log.debug(f"Sending PING request: {ping_request}")
            pong_response = self._ping_pong_stub.Ping(ping_request)
            self._log.debug(f"Received PONG response: {pong_response}")
            return True  # If no exception, the connection is valid
        except grpc.RpcError as e:
            self._log.warn(f"Error checking connection with PING/PONG: {e}")
            return False

    def _reconnect(self):
        for attempt in range(self._max_retries):
            self._log.debug(f"gRPC (re)connect attempt {attempt + 1}")
            try:
                self._channel = grpc.insecure_channel(self._address)
                self._nav_service_stub = nav_grpc.StreamlitNavigationServiceStub(self._channel)
                self._op_service_stub = op_grpc.StreamlitOperationServiceStub(self._channel)
                self._ping_pong_stub = pingpong_grpc.StreamlitPingPongServiceStub(self._channel)
                if self._check_connection():
                    return  # Successfully reconnected
            except grpc.RpcError as e:
                self._log.warn(f"Failed to reconnect to gRPC server on attempt {attempt + 1}: {e}")

            if attempt < self._max_retries - 1:
                # Calculate the next delay with jitter
                next_delay = min(self._base_delay * (2 ** attempt), self._max_delay)
                next_delay = random.uniform(0, next_delay)
                self._log.debug(f"Retrying connection in {next_delay} seconds...")
                time.sleep(next_delay)

        # All retries exhausted
        msg = f"Failed to reconnect to gRPC server {self._address} after {self._max_retries} attempts"
        self._log.error(msg)
        raise OSError(msg)

    def _ensure_connection(self):
        if not self._check_connection():
            self._reconnect()

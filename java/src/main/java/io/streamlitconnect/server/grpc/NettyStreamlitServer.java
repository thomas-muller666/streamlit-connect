package io.streamlitconnect.server.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.streamlitconnect.Config;
import io.streamlitconnect.StreamlitApp;
import io.streamlitconnect.StreamlitAppManager;
import io.streamlitconnect.StreamlitException;
import io.streamlitconnect.StreamlitServer;
import io.streamlitconnect.server.grpc.gen.StreamlitNavigationProto.StreamlitNavigation;
import io.streamlitconnect.server.grpc.gen.StreamlitNavigationProto.StreamlitNavigationRequest;
import io.streamlitconnect.server.grpc.gen.StreamlitNavigationServiceGrpc.StreamlitNavigationServiceImplBase;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationServiceGrpc.StreamlitOperationServiceImplBase;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.StreamlitOperation;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.StreamlitOperationsRequest;
import io.streamlitconnect.server.grpc.gen.StreamlitPingPongProto.PingRequest;
import io.streamlitconnect.server.grpc.gen.StreamlitPingPongProto.PongResponse;
import io.streamlitconnect.server.grpc.gen.StreamlitPingPongServiceGrpc.StreamlitPingPongServiceImplBase;
import io.streamlitconnect.utils.StringUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class NettyStreamlitServer implements StreamlitServer {

    private static final String KEY_SESSION_ID = "sessionId";
    private static final String KEY_SEQUENCE = "sequence";

    private static final String DEFAULT_PAGE_NAME = "default";

    private static class StreamlitPingPongServiceImpl extends StreamlitPingPongServiceImplBase {

        @Override
        public void ping(
            PingRequest request,
            StreamObserver<PongResponse> responseObserver
        ) {
            String sessionId = request.getSessionId();
            MDC.put(KEY_SESSION_ID, StringUtils.truncate(sessionId, 5));

            log.debug("Received ping request for session: {}", sessionId);

            PongResponse response = PongResponse.newBuilder()
                .setSessionId(sessionId)
                .build();

            log.debug("Sending pong response for session: {}: {}", sessionId, response);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            MDC.clear();
        }
    }

    private class StreamlitNavigationServiceImpl extends StreamlitNavigationServiceImplBase {

        @Override
        public void getNavigation(
            StreamlitNavigationRequest request,
            StreamObserver<StreamlitNavigation> responseObserver
        ) {
            String sessionId = request.getSessionId();
            int seq = request.getSeq();
            Utils.prepareMDC(sessionId, seq);
            log.debug("Received navigation request: {}", request);
            configureObserver(responseObserver, sessionId, seq);

            // Get an existing session context or create a new one if not present
            GrpcStreamlitSessionContext sessionContext = getSessionContext(sessionId, request.getApp());

            sessionContext.lock.lock();
            try {
                if (seq <= sessionContext.getCurrentSequenceNumber()) {
                    log.debug("Ignoring navigation request for session: {} seq: {}", sessionId, seq);
                    responseObserver.onError(new StreamlitException(
                        "Ignoring navigation request for session: " + sessionId + " seq: " + seq));
                    return;
                }

                sessionContext.setCurrentSequenceNumber(seq);

                // Create a new navigation request context
                GrpcNavigationRequestContext navContext = new GrpcNavigationRequestContext(
                    sessionContext,
                    seq,
                    request,
                    responseObserver);

                sessionContext.setCurrentNavReqContext(navContext);

                // Get the app
                StreamlitApp app = appManager.getOrCreateApp(sessionContext);

                // Run the navigation request
                requestExecutor.submit(() -> {
                    try {
                        navContext.handleRequest(app);
                    } catch(RuntimeException e) {
                        log.error("Error handling navigation request: {}", e.getMessage(), e);
                        navContext.cancel();
                        throw e;
                    }
                });

                // Wait for the navigation request to finish
                sessionContext.waitForNavReqToFinish();

            } finally {
                sessionContext.lock.unlock();
            }
            MDC.clear();
        }

        private void configureObserver(StreamObserver<StreamlitNavigation> responseObserver, String sessionId, long seq) {
            ServerCallStreamObserver<StreamlitNavigation> serverCallStreamObserver =
                (ServerCallStreamObserver<StreamlitNavigation>) responseObserver;

            serverCallStreamObserver.setOnCancelHandler(() -> {
                log.debug("Streamlit navigation stream cancelled for session: {} seq: {}", sessionId, seq);
            });
        }
    }

    private class StreamlitOperationServiceImpl extends StreamlitOperationServiceImplBase {

        @Override
        public void getOperations(
            StreamlitOperationsRequest request,
            StreamObserver<StreamlitOperation> responseObserver
        ) {
            String sessionId = request.getSessionId();
            int seq = request.getSeq();
            Utils.prepareMDC(sessionId, seq);
            log.debug("Received operation request: {}", request);
            configureObserver(responseObserver, sessionId, seq);

            GrpcStreamlitSessionContext sessionContext = getSessionContext(sessionId, request.getApp());

            sessionContext.lock.lock();
            try {
                if (seq < sessionContext.getCurrentSequenceNumber()) {
                    log.debug("Ignoring operations request for session: {} seq: {}", sessionId, seq);
                    responseObserver.onError(new StreamlitException(
                        "Ignoring operation request for session: " + sessionId + " seq: " + seq));
                    return;
                }

                sessionContext.setCurrentSequenceNumber(seq);

                // Create a new operations request context
                GrpcOperationsRequestContext opsContext = new GrpcOperationsRequestContext(
                    sessionContext,
                    seq,
                    request,
                    responseObserver);

                sessionContext.setCurrentOpsReqContext(opsContext);

                // Get the app
                StreamlitApp app = appManager.getOrCreateApp(sessionContext);

                // Run the ops request
                requestExecutor.submit(() -> {
                    try {
                        opsContext.handleRequest(app);
                    } catch (RuntimeException e) {
                        log.error("Error handling ops request: {}", e.getMessage(), e);
                        opsContext.cancel();
                        throw e;
                    }
                });

                // Wait for the ops request to finish
                log.debug("Waiting for ops request '{}' to finish for session: {}", request, sessionId);
                sessionContext.waitForOpsReqToFinish();
                log.debug("Ops request '{}' finished for session: {}", request, sessionId);

            } finally {
                sessionContext.lock.unlock();
            }

            MDC.clear();
        }

        private void configureObserver(StreamObserver<StreamlitOperation> responseObserver, String sessionId, long seq) {
            ServerCallStreamObserver<StreamlitOperation> serverCallStreamObserver =
                (ServerCallStreamObserver<StreamlitOperation>) responseObserver;

            serverCallStreamObserver.setOnCancelHandler(() -> {
                log.debug("Streamlit operation stream cancelled for session: {} seq: {}", sessionId, seq);
            });
        }

    }

    private static final Logger log = LoggerFactory.getLogger(NettyStreamlitServer.class);

    // sessionId -> GrpcStreamlitContext
    private final ConcurrentHashMap<String, GrpcStreamlitSessionContext> sessions = new ConcurrentHashMap<>();

    private final AtomicReference<Server> server = new AtomicReference<>();

    private StreamlitAppManager appManager;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final ExecutorService requestExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());

    public NettyStreamlitServer() {
    }

    @Override
    public void start(@NonNull StreamlitAppManager appManager, @NonNull Config config) {
        try {
            boolean started = server.compareAndSet(
                null,
                ServerBuilder.forPort(config.getGrpcServerPort())
                    .addService(new NettyStreamlitServer.StreamlitPingPongServiceImpl())
                    .addService(new NettyStreamlitServer.StreamlitOperationServiceImpl())
                    .addService(new NettyStreamlitServer.StreamlitNavigationServiceImpl())
                    .build()
                    .start()
            );
            if (!started) {
                log.error("Server already started");
                throw new StreamlitException("Server already started");
            }
            isRunning.set(true);
        } catch (IOException e) {
            log.error("Error staring server: {}", e.getMessage(), e);
            throw new StreamlitException(e);
        }

        log.info("Streamlit gRPC server started successfully on port {}", config.getGrpcServerPort());

        this.appManager = appManager;

        scheduleEviction(config);

        log.info("Awaiting termination...");
        try {
            server.get().awaitTermination();
        } catch (InterruptedException e) {
            Thread.interrupted(); // Restore the interrupted status
            if (isRunning.get()) {
                log.error("Server thread interrupted: {}", e.getMessage(), e);
                throw new StreamlitException(e);
            }
        }
    }

    @Override
    public void stop() {
        if (server.get() != null && (!server.get().isShutdown()) && isRunning.compareAndSet(true, false)) {
            try {
                server.get().shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Error stopping gRPC server: {}", e.getMessage(), e);
                throw new StreamlitException(e);
            } finally {
                sessions.forEach((sessionId, context) -> {
                    context.close();
                });
                scheduler.shutdown();
                sessions.clear();
                server.set(null);
            }
            log.info("Streamlit gRPC server stopped");
        }
    }

    private @NotNull GrpcStreamlitSessionContext getSessionContext(String sessionId, String appName
    ) {
        if (org.apache.commons.lang3.StringUtils.isBlank(appName)) {
            appName = null;
        }

        final String fAppName = appName;

        // Get an existing context or create a new one if not present
        GrpcStreamlitSessionContext context = sessions.computeIfAbsent(
            sessionId,
            key -> new GrpcStreamlitSessionContext(sessionId, fAppName)
        );

        log.debug("Context for session: {} is:\n{}", sessionId, context);

        return context;
    }

    private void scheduleEviction(Config config) {
        // Schedule a task to run every minute that removes inactive sessions
        Runnable evictor = () -> {
            sessions.forEach((sessionId, context) -> {
                possiblyEvict(config, sessionId, context);

            });
        };

        ScheduledFuture<?> evictorHandle = scheduler.scheduleAtFixedRate(evictor, 1, 1, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> evictorHandle.cancel(true)));
    }

    private void possiblyEvict(Config config, String sessionId, GrpcStreamlitSessionContext context) {
        if (context.getLastActivityAt() == null || !isInactive(context, config)) {
            return; // Not an inactive session
        }

        appManager.disposeSession(context.getSessionId());
        sessions.remove(sessionId);
        context.close();
        log.info("Evicted inactive session: {}", sessionId);
    }

    private boolean isInactive(GrpcStreamlitSessionContext context, Config config) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(
            Instant.now().toEpochMilli() - context.getLastActivityAt().toEpochMilli());
        return seconds >= config.getEvictionTimeoutSeconds();
    }
}


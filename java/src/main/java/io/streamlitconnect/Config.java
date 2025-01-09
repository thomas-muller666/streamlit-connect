package io.streamlitconnect;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Config {

    /**
     * The default port for the gRPC server when Streamlit (Python) is used as an intermediate server and gRPC client.
     */
    public static final int DEFAULT_GRPC_SERVER_PORT = 50051;

    public static final int DEFAULT_EVICTION_TIMEOUT_SECONDS = 5 * 60;


    private boolean serveFrontendApp; // Not yet implemented

    @Builder.Default
    private int grpcServerPort = DEFAULT_GRPC_SERVER_PORT;

    @Builder.Default
    private int evictionTimeoutSeconds = DEFAULT_EVICTION_TIMEOUT_SECONDS;

}

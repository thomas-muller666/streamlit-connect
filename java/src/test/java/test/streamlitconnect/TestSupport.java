package test.streamlitconnect;

import io.streamlitconnect.AppCache;
import io.streamlitconnect.Config;
import io.streamlitconnect.StreamlitAppManager;
import io.streamlitconnect.StreamlitServer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestSupport {

    private static final Logger log = LoggerFactory.getLogger(TestSupport.class);

    public static void startServer(@NonNull StreamlitAppManager appManager) {
        AppCache appCache = new AppCache(appManager);

        var config = Config.builder()
            .grpcServerPort(Config.DEFAULT_GRPC_SERVER_PORT)
            .build();

        StreamlitServer server = StreamlitServer.getDefault();

        log.debug("Starting Streamlit gRPC server: {} with config: {}", server, config);
        server.start(appCache, config);
    }

}

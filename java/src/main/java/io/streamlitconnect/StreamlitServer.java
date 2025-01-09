package io.streamlitconnect;

import java.lang.reflect.InvocationTargetException;
import lombok.NonNull;

/**
 * Represents a Streamlit server that can be started and stopped.
 */
public interface StreamlitServer {

    /**
     * Get the default Streamlit server.
     *
     * @return The default Streamlit server.
     */
    static StreamlitServer getDefault() {
        try {
            return (StreamlitServer) Class.forName("io.streamlitconnect.server.grpc.NettyStreamlitServer")
                .getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new StreamlitException("Failed to create the default Streamlit server", e);
        }
    }

    default void start(@NonNull StreamlitAppManager appManager) {
        start(appManager, Config.builder().build());
    }

    /**
     * Start the Streamlit server. This method will start the Streamlit server and use the given app factory and the given
     * configuration. Note that this method will block the current thread until the server is stopped.
     *
     * @param appManager The factory for creating a (single-page or multi-page) Streamlit app per session.
     * @param config     The configuration for the Streamlit server.
     */
    void start(@NonNull StreamlitAppManager appManager, Config config);

    /**
     * Stop the Streamlit server,
     */
    void stop();

}

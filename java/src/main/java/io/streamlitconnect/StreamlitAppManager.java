package io.streamlitconnect;

import lombok.NonNull;

/**
 * An interface for managing Streamlit applications.
 */
public interface StreamlitAppManager {

    /**
     * Returns the Streamlit application for the given context, creating it if necessary.
     *
     * @param context The Streamlit context.
     * @return The Streamlit application.
     */
    @NonNull
    StreamlitApp getOrCreateApp(@NonNull StreamlitSessionContext context);

    /**
     * Disposes of the Streamlit application session with the given session id. The default implementation does nothing.
     *
     * @param sessionId The session id.
     */
    default void disposeSession(@NonNull String sessionId) {
        // Default implementation does nothing
    }

}

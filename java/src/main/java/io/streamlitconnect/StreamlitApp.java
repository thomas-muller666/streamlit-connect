package io.streamlitconnect;

import java.io.Closeable;
import lombok.NonNull;

/**
 * Interface for Streamlit applications.
 */
public interface StreamlitApp extends Closeable {

    /**
     * Returns the navigation menu for the Streamlit application. Note that the StreamlitRequestContext is unique to each
     * request/response transaction. The default implementation returns null (i.e. no navigation menu).
     *
     * @param context The Streamlit navigation request context.
     * @return The navigation menu for the Streamlit application.
     */
    default NavigationMenu getNavigationMenu(NavigationRequestContext context) {
        return null;
    }

    /**
     * Renders the Streamlit application. Note that the given OperationsRequestContext is unique to each request/response
     * transaction.
     *
     * @param context The Streamlit context.
     */
    void render(@NonNull OperationsRequestContext context);

    /**
     * Closes the Streamlit application. The default implementation does nothing.
     */
    default void close() {
        // Default implementation does nothing
    }

}

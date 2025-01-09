package io.streamlitconnect;


import io.streamlitconnect.widgets.Widget;
import java.io.Closeable;
import java.time.Instant;
import lombok.NonNull;

/**
 * StreamlitContext interface represents the context for a Streamlit session. An instance of this interface is created for each
 * session and stays alive for the duration of the session, i.e. spanning multiple req/res transactions.
 */
public interface StreamlitSessionContext extends Closeable {

    /**
     * This method returns the unique identifier for the Streamlit application session.
     *
     * @return The unique identifier for the Streamlit application session.
     */
    String getSessionId();

    /**
     * This method returns the name of the Streamlit application.
     *
     * @return The name of the Streamlit application.
     */
    String getAppName();

    /**
     * Returns the widget with the given key, or null if the widget does not exist. Widgets are automatically added to the session
     * when adding them to the root container, the sidebar container or any other created container for the application.
     *
     * @param key The key of the widget to retrieve.
     * @return The widget with the given key, or null if the widget does not exist.
     */
    Widget<?> getWidget(String key);

    /**
     * Returns the time at which the session was last interacted with.
     */
    Instant getLastActivityAt();

    /**
     * This method returns the value of an attribute that was previously set using the {@link #setAttribute(String, Object)}
     * method.
     *
     * @param name The name of the attribute to retrieve.
     * @return The value of the attribute, or null if the attribute does not exist.
     */
    Object getAttribute(@NonNull String name);

    /**
     * This method sets an attribute for the Streamlit req/res transaction. This attribute can be retrieved later using the
     * {@link #getAttribute(String)} method. Note that the attribute is only valid for the current req/res transaction.
     *
     * @param name  The name of the attribute to set.
     * @param value The value of the attribute to set.
     */
    void setAttribute(@NonNull String name, Object value);

}

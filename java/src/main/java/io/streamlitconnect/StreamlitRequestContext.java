package io.streamlitconnect;


import java.io.Closeable;
import java.time.Instant;
import java.util.concurrent.Executor;
import lombok.NonNull;

/**
 * The StreamlitContext interface represents the (super) context for a Streamlit req/res transaction.
 */
public interface StreamlitRequestContext extends Executor, Closeable {

    /**
     * This method returns the StreamlitSessionContext object that represents the current Streamlit application session.
     *
     * @return The StreamlitSessionContext object that represents the current Streamlit application session.
     */
    StreamlitSessionContext getSessionContext();

    /**
     * This method returns the unique sequnce number identifier for the Streamlit request.
     *
     * @return The unique sequence number identifier for the Streamlit request.
     */
    int getSequenceNumber();

    /**
     * Returns the time at which the request was created.
     */
    Instant getCreatedAt();

    /**
     * Returns the time at which the request was closed.
     */
    Instant getClosedAt();

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

    /**
     * Runs a command in the Streamlit req/res transaction. The framework should run this in a separate (virtual thread) to avoid
     * blocking the execution thread. Note that the command will be attempted cancelled if a new request is received before the
     * command is completed irrespective of the command's state.
     *
     * @param command the runnable task
     */
    void execute(@NonNull Runnable command);

    /**
     * Returns a boolean indicating whether the Streamlit req/res transaction has been closed.
     */
    boolean isClosed();

    /**
     * Returns a boolean indicating whether the Streamlit req/res transaction has been cancelled.
     */
    boolean isCancelled();
}

package io.streamlitconnect;

/**
 * Represents an exception that occurred while interacting with Streamlit.
 */
public class StreamlitException extends RuntimeException {

    public StreamlitException() {
        super();
    }

    public StreamlitException(Throwable cause) {
        super(cause);
    }

    public StreamlitException(String message) {
        super(message);
    }

    public StreamlitException(String message, Throwable cause) {
        super(message, cause);
    }

}

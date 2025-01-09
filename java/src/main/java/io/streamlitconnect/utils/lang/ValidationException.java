package io.streamlitconnect.utils.lang;

/**
 * Exception that is thrown when a validation error occurs.
 */
public class ValidationException extends RuntimeException {

    /**
     * Creates a new ValidationException.
     */
    public ValidationException() {
        super();
    }

    /**
     * Creates a new ValidationException with the specified detail message.
     *
     * @param message The detail message.
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Creates a new ValidationException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause   The cause of the exception.
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ValidationException with the specified cause.
     *
     * @param cause The cause of the exception.
     */
    public ValidationException(Throwable cause) {
        super(cause);
    }
}

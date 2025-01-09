package io.streamlitconnect.utils.lang;

public interface Validator<T> {

    /**
     * Validates the given object.
     *
     * @param t the object to validate
     * @throws ValidationException if the object is not valid
     */
    void validate(T t) throws ValidationException;
}

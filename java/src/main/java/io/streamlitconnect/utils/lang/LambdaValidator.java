package io.streamlitconnect.utils.lang;

import java.util.function.Predicate;
import lombok.NonNull;

public class LambdaValidator<T> implements Validator<T> {

    private final Predicate<T> validationPredicate;

    public LambdaValidator(@NonNull Predicate<T> validationPredicate) {
        this.validationPredicate = validationPredicate;
    }

    @Override
    public void validate(T t) {
        if (!validationPredicate.test(t)) {
            throw new ValidationException(
                String.format("Validation failed for predicate: %s with value: %s", validationPredicate, t));
        }
    }
}

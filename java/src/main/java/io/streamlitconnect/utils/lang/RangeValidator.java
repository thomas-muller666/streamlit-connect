package io.streamlitconnect.utils.lang;

public class RangeValidator<P extends Comparable<P>> implements Validator<P> {

    private final P inclusiveMin;

    private final P exclusiveMin;

    private final P inclusiveMax;

    private final P exclusiveMax;

    public RangeValidator(P inclusiveMin, P exclusiveMin, P inclusiveMax, P exclusiveMax) {
        this.inclusiveMin = inclusiveMin;
        this.exclusiveMin = exclusiveMin;
        this.inclusiveMax = inclusiveMax;
        this.exclusiveMax = exclusiveMax;

        if (inclusiveMin != null && exclusiveMin != null) {
            throw new IllegalArgumentException("Both inclusiveMin '" + inclusiveMin + "' and exclusiveMin '" +
                exclusiveMin + "' are set. They are mutually exclusive.");
        }

        if (inclusiveMax != null && exclusiveMax != null) {
            throw new IllegalArgumentException("Both inclusiveMax '" + inclusiveMax + "' and exclusiveMax '" +
                exclusiveMax + "' are set. They are mutually exclusive.");
        }
    }

    @Override
    public void validate(P t) {
        if (inclusiveMin != null && t.compareTo(inclusiveMin) < 0) {
            throw new ValidationException("Value '" + t + "' is below the allowed minimum '" + inclusiveMin + "'");
        }

        if (exclusiveMin != null && t.compareTo(exclusiveMin) <= 0) {
            throw new ValidationException("Value '" + t + "' is below the allowed minimum '" + exclusiveMin + "'");
        }

        if (inclusiveMax != null && t.compareTo(inclusiveMax) > 0) {
            throw new ValidationException("Value '" + t + "' is above the allowed maximum '" + inclusiveMax + "'");
        }

        if (exclusiveMax != null && t.compareTo(exclusiveMax) >= 0) {
            throw new ValidationException("Value '" + t + "' is above the allowed maximum '" + exclusiveMax + "'");
        }
    }
}
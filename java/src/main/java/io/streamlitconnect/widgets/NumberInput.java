package io.streamlitconnect.widgets;

import static org.apache.commons.lang3.Validate.isTrue;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


/**
 * Number input widget.
 * <p>
 * Valid formatters: %d %e %f %g %i %u
 */
@Getter
@Setter
public class NumberInput extends Widget<Number> {

    private String placeholder;

    private Number min = null, max = null, step = null;

    // Valid formatters: %d %e %f %g %i %u
    private String format;

    public NumberInput(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public boolean setValue(Number value) {
        checkIntegerOrFloat(value);
        return super.setValue(value);
    }

    public void setMin(Number min) {
        checkIntegerOrFloat(min);
        this.min = min;
    }

    public void setMax(Number max) {
        checkIntegerOrFloat(max);
        this.max = max;
    }

    public void setStep(Number step) {
        checkIntegerOrFloat(step);
        this.step = step;
    }

    private void checkIntegerOrFloat(Number value) {
        isTrue(value instanceof Integer || value instanceof Float, "Value must be an Integer or a Float");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("min", min)
            .append("max", max)
            .append("step", step)
            .append("format", format)
            .append("placeholder", placeholder)
            .toString();
    }

}

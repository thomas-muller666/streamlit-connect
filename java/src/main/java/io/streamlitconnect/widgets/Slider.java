package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
public abstract class Slider<T, STEP> extends Widget<T> {

    private T min, max;

    private STEP step;

    private String format;

    protected Slider(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("min", min)
            .append("max", max)
            .append("step", step)
            .append("format", format)
            .toString();
    }

}

package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@Setter
public abstract class RangeSlider<T, STEP> extends Widget<Pair<T, T>> {

    private T min, max;

    private STEP step;

    private String format;

    protected RangeSlider(@NonNull String label) {
        super(label, true, true, false, true);
    }

    public T getFromValue() {
        return getValue().getLeft();
    }

    public void setFromValue(T fromValue) {
        setValue(MutablePair.of(fromValue, getValue().getRight()));
    }

    public T getToValue() {
        return getValue().getRight();
    }

    public void setToValue(T toValue) {
        setValue(MutablePair.of(getValue().getLeft(), toValue));
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

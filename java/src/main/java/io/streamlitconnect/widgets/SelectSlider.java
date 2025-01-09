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
public class SelectSlider extends Widget<Pair<Integer, Integer>> {

    public static final String DEFAULT_PLACEHOLDER = "Select an option";

    private String[] options = new String[0];

    public SelectSlider(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public void reset() {
        super.reset();
        value = MutablePair.of(0, -1);
        previousValue = MutablePair.of(0, -1);
    }

    public String getLowerOption() {
        Integer lowerIndex = value.getLeft();
        return (lowerIndex == null || lowerIndex < 0 || lowerIndex >= options.length) ? null : options[lowerIndex];
    }

    public String getUpperOption() {
        Integer upperIndex = value.getRight();
        return (upperIndex == null || upperIndex < 0 || upperIndex >= options.length) ? null : options[upperIndex];
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("options", options)
            .append("lowerIndex", getValue().getLeft())
            .append("upperIndex", getValue().getRight())
            .toString();
    }

}

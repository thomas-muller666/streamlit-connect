package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
public class Selectbox extends Widget<Integer> {

    public static final String DEFAULT_PLACEHOLDER = "Select an option";

    private String[] options = new String[0];

    private String placeholder = DEFAULT_PLACEHOLDER; // Showed if no options are chosen

    public Selectbox(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public void reset() {
        super.reset();
        value = 0;
        previousValue = 0;
    }

    public String getSelectedOption() {
        return (value < 0 || value >= options.length) ? null : options[value];
    }

    public String getPreviousSelectedOption() {
        return (previousValue < 0 || previousValue >= options.length) ? null : options[previousValue];
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("options", options)
            .append("placeholder", placeholder)
            .toString();
    }

}

package io.streamlitconnect.widgets;

import java.util.Arrays;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
public class Multiselect extends Widget<int[]> {

    public static final String DEFAULT_PLACEHOLDER = "Select an option";

    @NonNull
    private String[] options = new String[0];

    private String placeholder = DEFAULT_PLACEHOLDER; // Showed if no options are chosen

    public Multiselect(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public void reset() {
        super.reset();
        value = new int[0];
        previousValue = new int[0];
    }

    public String[] getSelectedOptions() {
        String[] selectedOptions = new String[value.length];
        for (int i = 0; i < value.length; i++) {
            selectedOptions[i] = options[value[i]];
        }
        return selectedOptions;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("options", options)
            .append("selectedIndices", Arrays.toString(value))
            .append("placeholder", placeholder)
            .toString();
    }

}

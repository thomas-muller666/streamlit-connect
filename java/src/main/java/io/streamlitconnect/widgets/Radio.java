package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
@Setter
public class Radio extends Widget<Integer> {

    public static final String DEFAULT_LABEL = "Choose an option";

    private String[] options = new String[0];

    boolean horizontal;

    private String[] captions = new String[0];

    public Radio() {
        this(DEFAULT_LABEL);
    }

    public Radio(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public void reset() {
        super.reset();
        value = 0;
        previousValue = 0;
    }

    public String getSelectedOption() {
        return (value == null || value < 0 || value >= options.length) ? null : options[value];
    }

    public String getSelectedCaption() {
        return (value == null || value < 0 || value >= captions.length) ? null : captions[value];
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("options", options)
            .append("captions", captions)
            .toString();
    }


}

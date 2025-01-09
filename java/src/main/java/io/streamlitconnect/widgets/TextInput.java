package io.streamlitconnect.widgets;

import io.streamlitconnect.utils.StringUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A TextInput represents the interface for interacting with a corresponding Streamlit text input.
 * See <a href="https://docs.streamlit.io/develop/api-reference/widgets/st.text_input">Streamlit text input API doc</a>
 */
@Setter
@Getter
public class TextInput extends Widget<String> {

    public enum TextInputType {
        DEFAULT,
        PASSWORD,
    }

    private TextInputType type = TextInputType.DEFAULT;

    private String autocomplete;

    private String placeholder;

    private int maxChars;

    public TextInput(@NonNull String label) {
        this(label, null);
    }

    public TextInput(@NonNull String label, String help) {
        super(label, true, true, false, true);
        this.help = help;
    }

    @Override
    public void reset() {
        super.reset();
        value = StringUtils.EMPTY_STRING;
        previousValue = StringUtils.EMPTY_STRING;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("type", type)
            .append("autocomplete", autocomplete)
            .append("placeholder", placeholder)
            .append("maxChars", maxChars)
            .toString();
    }

}

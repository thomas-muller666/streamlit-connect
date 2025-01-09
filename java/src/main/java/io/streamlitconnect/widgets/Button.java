package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A Button represents the interface for interacting with a corresponding Streamlit button.
 * <p>
 * See corresponding
 * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#button">Streamlit API doc</a>}
 */
@Setter
@Getter
public class Button extends Widget<Void> {

    public enum ButtonType {
        PRIMARY,
        SECONDARY
    }

    private ButtonType type = ButtonType.SECONDARY;

    public Button(@NonNull String label) {
        this(label, null);
    }

    public Button(@NonNull String label, String help) {
        super(label, true, false, true, true);
        this.help = help;
    }

    protected Button(@NonNull String label, String help, boolean changeCallbackSupported) {
        super(label, true, false, true, changeCallbackSupported);
        this.help = help;
    }

    public boolean setValue(Void t) {
        setChanged(true);
        return true;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("type", type)
            .toString();
    }

}

package io.streamlitconnect.widgets;

import io.streamlitconnect.utils.StringUtils;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Base class for all Streamlit widgets.
 * @param <T> The type of the widget value
 */
@Getter
public abstract class Widget<T> {

    public enum LabelVisibility {
        VISIBLE,
        HIDDEN,
        COLLAPSED
    }

    protected T value;

    @Setter
    protected T previousValue;

    @Setter(AccessLevel.PACKAGE)
    private boolean changed;

    private final String key;

    private final boolean helpSupported;

    private final boolean labelVisibilitySupported;

    private final boolean useContainerWidthSupported;

    private final boolean changeCallbackSupported;

    @Setter
    private boolean useContainerWidth;

    @Setter
    protected LabelVisibility labelVisibility = LabelVisibility.VISIBLE;

    @Setter
    protected String label;

    @Setter
    protected boolean disabled;

    @Setter
    protected String help;

    protected Widget(@NonNull String label) {
        this(label, true);
    }

    protected Widget(@NonNull String label, boolean helpSupported) {
        this(label, helpSupported, false, false, true);
    }

    protected Widget(
        @NonNull String label,
        boolean helpSupported,
        boolean labelVisibilitySupported,
        boolean useContainerWidthSupported,
        boolean changeCallbackSupported
    ) {
        this.key = Widget.class.getSimpleName() + "_" + StringUtils.randomNumber(6);
        this.label = label;
        this.helpSupported = helpSupported;
        this.labelVisibilitySupported = labelVisibilitySupported;
        this.useContainerWidthSupported = useContainerWidthSupported;
        this.changeCallbackSupported = changeCallbackSupported;
        reset();
    }

    /**
     * Callback to be called when the widget value changes.
     *
     * @param args   List of arguments (if any)
     * @param kwargs Map of keyword arguments (if any)
     */
    public void onChange(List<String> args, Map<String, String> kwargs) {
        // Default implementation does nothing
    }

    public void reset() {
        this.value = null;
        this.previousValue = null;
        this.changed = false;
    }

    /**
     * Set the value of the widget. If the value has changed, the previous value is stored and the touched flag is set to true.
     *
     * @param t The new value
     * @return true if the value has changed, false otherwise
     */
    public boolean setValue(T t) {
        // Check if changed
        if ((t == null && value == null) || (t != null && t.equals(value))) {
            return false;
        }
        previousValue = value;
        value = t;
        changed = true;
        return true;
    }

    /**
     * Reset the touched flag.
     */
    public void resetChanged() {
        changed = false;
    }

    /**
     * Two widgets are equal if they have the same key.
     *
     * @param obj The object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Widget<?> other = (Widget<?>) obj;
        return this.key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("key", getKey())
            .append("label", label)
            .append("changed", changed)
            .append("value", value)
            .append("previousValue", previousValue)
            .append("help", help)
            .append("disabled", disabled)
            .append("labelVisibility", labelVisibility)
            .append("useContainerWidth", useContainerWidth)
            .append("helpSupported", helpSupported)
            .append("labelVisibilitySupported", labelVisibilitySupported)
            .append("useContainerWidthSupported", useContainerWidthSupported)
            .append("changeCallbackSupported", changeCallbackSupported)
            .toString();
    }


}

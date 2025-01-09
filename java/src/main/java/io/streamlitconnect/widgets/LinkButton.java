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
public class LinkButton extends Button {

    private String url;

    public LinkButton(@NonNull String label, @NonNull String url) {
        this(label, url, null);
    }

    public LinkButton(@NonNull String label, @NonNull String url, String help) {
        super(label, help, false);
        this.url = url;
    }

    public boolean setValue(Void t) {
        throw new UnsupportedOperationException("setValue is not supported for LinkButton");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("url", url)
            .toString();
    }

}

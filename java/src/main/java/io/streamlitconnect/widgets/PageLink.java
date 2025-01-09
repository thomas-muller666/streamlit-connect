package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;

/**
 * A link to another page
 * <p>
 * See corresponding
 * <a href="https://docs.streamlit.io/develop/api-reference/widgets/st.page_link">st.page_link</a>} Streamlit API doc.
 */
@Setter
@Getter
public class PageLink extends Widget<Void> {

    @NonNull
    private String pageName;

    private String icon;

    public PageLink(@NotNull String pageName, @NonNull String label) {
        this(pageName, label, null);
    }

    public PageLink(@NotNull String pageName, @NonNull String label, String icon) {
        super(label, true, false, true, false);
        this.pageName = pageName;
        this.icon = icon;
    }

    public boolean setValue(Void t) {
        throw new UnsupportedOperationException("setValue is not supported for PageLink");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("pageName", pageName)
            .append("icon", icon)
            .toString();
    }

}

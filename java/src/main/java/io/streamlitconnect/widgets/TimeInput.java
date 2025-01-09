package io.streamlitconnect.widgets;

import java.time.LocalTime;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A TimeInput represents the interface for interacting with a corresponding Streamlit time input.
 * See <a href="https://docs.streamlit.io/develop/api-reference/widgets/st.time_input">Streamlit time input API doc</a>
 */
@Getter
@Setter
public class TimeInput extends Widget<LocalTime> {

    private int stepSeconds = 900; // 15 minutes

    public TimeInput(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("stepSeconds", stepSeconds)
            .toString();
    }

}

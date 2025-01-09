package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A Toggle represents the interface for interacting with a corresponding Streamlit toggle.
 * See <a href="https://docs.streamlit.io/develop/api-reference/widgets/st.checkbox">Streamlit toggle API doc</a>
 */
@Getter
@Setter
public class Toggle extends Widget<Boolean> {

    public Toggle(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public void reset() {
        super.reset();
        value = false;
        previousValue = false;
    }

}

package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A Checkbox represents the interface for interacting with a corresponding Streamlit checkbox.
 * See <a href="https://docs.streamlit.io/develop/api-reference/widgets/st.checkbox">Streamlit checkbox API doc</a>
 */
@Getter
@Setter
public class Checkbox extends Widget<Boolean> {

    public Checkbox(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public void reset() {
        super.reset();
        value = false;
        previousValue = false;
    }

}

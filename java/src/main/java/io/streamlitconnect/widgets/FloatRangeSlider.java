package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 * A FloatRangeSlider represents the interface for interacting with a corresponding Streamlit float range slider.
 * <p>
 * See corresponding
 * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#slider">Streamlit API doc</a>}
 */
@Getter
@Setter
public class FloatRangeSlider extends RangeSlider<Float, Float> {

    public FloatRangeSlider(@NonNull String label) {
        super(label);
        setValue(MutablePair.of(0.25f, 0.50f));
        setMin(0.0f);
        setMax(1.0f);
        setStep(0.01f);
    }

}

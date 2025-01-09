package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * An IntegerSlider represents the interface for interacting with a corresponding Streamlit integer slider.
 * <p>
 * See corresponding
 * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#slider">Streamlit API doc</a>}
 */
@Getter
@Setter
public class IntegerSlider extends Slider<Integer, Integer> {

    /**
     * Constructor for IntegerSlider. Defaults min to 0, max to 100, and step to 1. The initial value is set to 0.
     *
     * @param label The label of the IntegerSlider
     */
    public IntegerSlider(@NonNull String label) {
        super(label);
        setValue(0);
        setMin(0);
        setMax(100);
        setStep(1);
    }

}

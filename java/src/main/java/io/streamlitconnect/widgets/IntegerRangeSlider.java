package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 * An IntegerRangeSlider represents the interface for interacting with a corresponding Streamlit integer range slider.
 * <p>
 * See corresponding
 * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#slider">Streamlit API doc</a>}
 */
@Getter
@Setter
public class IntegerRangeSlider extends RangeSlider<Integer, Integer> {

    /**
     * Constructor for IntegerRangeSlider. Defaults min to 0, max to 100, and step to 1. Initial range value is set to 25-50.
     *
     * @param label The label of the IntegerRangeSlider
     */
    public IntegerRangeSlider(@NonNull String label) {
        super(label);
        setValue(MutablePair.of(25, 50));
        setMin(0);
        setMax(100);
        setStep(1);
    }

}

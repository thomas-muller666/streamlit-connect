package io.streamlitconnect.widgets;

import java.time.LocalTime;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 * A TimeRangeSlider represents the interface for interacting with a corresponding Streamlit time range slider.
 * See corresponding {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#slider">Streamlit API doc</a>}
 */
public class TimeRangeSlider extends RangeSlider<LocalTime, Integer> {

    public TimeRangeSlider(@NonNull String label) {
        super(label);
        setValue(MutablePair.of(LocalTime.now().minusHours(1), LocalTime.now().plusHours(1)));
        setMin(LocalTime.of(0, 0, 0));
        setMax(LocalTime.of(23, 45, 0));
        setStep(60 * 15); // 15 minutes
    }

}

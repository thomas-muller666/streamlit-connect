package io.streamlitconnect.widgets;

import java.time.LocalTime;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A TimeSlider represents the interface for interacting with a corresponding Streamlit time slider.
 * See <a href="https://docs.streamlit.io/develop/api-reference/widgets/st.time_input">Streamlit time slider API doc</a>
 */
@Getter
@Setter
public class TimeSlider extends Slider<LocalTime, Integer> {

    /**
     * Constructor for TimeSlider. Defaults min to 12 hours before now, max to 12 hours after now, and step to 15 minutes.
     * The initial value is set to current time (local).
     *
     * @param label The label of the TimeSlider
     */
    public TimeSlider(@NonNull String label) {
        super(label);
        setValue(LocalTime.now());
        setMin(LocalTime.now().minusHours(12));
        setMax(LocalTime.now().plusHours(12));
        setStep(60 * 15); // 15 minutes
    }

}

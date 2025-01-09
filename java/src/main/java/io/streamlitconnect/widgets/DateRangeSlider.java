package io.streamlitconnect.widgets;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 * A DateInput represents the interface for interacting with a corresponding Streamlit date input.
 * See <a href="https://docs.streamlit.io/develop/api-reference/widgets/st.date_input">Streamlit date input API doc</a>
 */
@Getter
@Setter
public class DateRangeSlider extends RangeSlider<LocalDate, Integer> {

    public DateRangeSlider(@NonNull String label) {
        super(label);
        setValue(MutablePair.of(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
        setMin(LocalDate.now().minusDays(10));
        setMax(LocalDate.now().plusDays(10));
        setStep(60 * 60 * 24);
    }

}

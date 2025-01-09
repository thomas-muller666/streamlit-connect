package io.streamlitconnect.widgets;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class DateSlider extends Slider<LocalDate, Integer> {

    public DateSlider(@NonNull String label) {
        super(label);
        setValue(LocalDate.now());
        setMin(LocalDate.now().minusDays(10));
        setMax(LocalDate.now().plusDays(10));
        setStep(60 * 60 * 24);
    }

}

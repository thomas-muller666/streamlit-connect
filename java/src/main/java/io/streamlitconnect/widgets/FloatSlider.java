package io.streamlitconnect.widgets;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class FloatSlider extends Slider<Float, Float> {

    public FloatSlider(@NonNull String label) {
        super(label);
        setValue(0.0f);
        setMin(0.0f);
        setMax(1.0f);
        setStep(0.01f);
    }

}

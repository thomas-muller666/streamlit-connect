package io.streamlitconnect.server.grpc;

import static org.apache.commons.lang3.Validate.isTrue;

import io.streamlitconnect.StreamlitException;
import io.streamlitconnect.server.grpc.gen.StreamlitCommonsProto.IntOrFloat;
import io.streamlitconnect.server.grpc.gen.StreamlitCommonsProto.ValueRange;
import io.streamlitconnect.server.grpc.gen.StreamlitCommonsProto.ValueSingle;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.ButtonOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.ButtonOp.Builder;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.CheckboxOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.DateInputOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.MultiselectOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.NumberInputOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.PageLinkOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.RadioOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.SelectSliderOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.SelectboxOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.SliderOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.TextInputOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.TimeInputOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.ToggleOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.WidgetProperties;
import io.streamlitconnect.widgets.Button;
import io.streamlitconnect.widgets.Checkbox;
import io.streamlitconnect.widgets.DateInput;
import io.streamlitconnect.widgets.DateRangeSlider;
import io.streamlitconnect.widgets.DateSlider;
import io.streamlitconnect.widgets.FloatRangeSlider;
import io.streamlitconnect.widgets.FloatSlider;
import io.streamlitconnect.widgets.IntegerRangeSlider;
import io.streamlitconnect.widgets.IntegerSlider;
import io.streamlitconnect.widgets.LinkButton;
import io.streamlitconnect.widgets.Multiselect;
import io.streamlitconnect.widgets.NumberInput;
import io.streamlitconnect.widgets.PageLink;
import io.streamlitconnect.widgets.Radio;
import io.streamlitconnect.widgets.RangeSlider;
import io.streamlitconnect.widgets.SelectSlider;
import io.streamlitconnect.widgets.Slider;
import io.streamlitconnect.widgets.TextInput;
import io.streamlitconnect.widgets.TimeInput;
import io.streamlitconnect.widgets.TimeRangeSlider;
import io.streamlitconnect.widgets.TimeSlider;
import io.streamlitconnect.widgets.Toggle;
import io.streamlitconnect.widgets.Widget;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class WidgetMapper {

    static ButtonOp toButtonOp(@NonNull Button button, @NonNull String containerKey) {
        Builder builder = ButtonOp.newBuilder()
            .setWidgetProps(toWidgetProperties(button, containerKey))
            .setTypeValue(button.getType().ordinal());

        if (button instanceof LinkButton linkButton) {
            builder.setUrl(linkButton.getUrl());
        }
        return builder.build();
    }

    static PageLinkOp toPageLinkOp(@NonNull PageLink pageLink, @NonNull String containerKey) {
        PageLinkOp.Builder builder = PageLinkOp.newBuilder()
            .setWidgetProps(toWidgetProperties(pageLink, containerKey))
            .setPage(pageLink.getPageName());

        if (pageLink.getIcon() != null) {
            builder.setIcon(pageLink.getIcon());
        }
        return builder.build();
    }

    static CheckboxOp toCheckboxOp(@NonNull Checkbox checkbox, @NonNull String containerKey) {
        CheckboxOp.Builder builder = CheckboxOp.newBuilder()
            .setWidgetProps(toWidgetProperties(checkbox, containerKey));
        return builder.build();
    }

    static ToggleOp toToggleOp(@NonNull Toggle toggle, @NonNull String containerKey) {
        ToggleOp.Builder builder = ToggleOp.newBuilder()
            .setWidgetProps(toWidgetProperties(toggle, containerKey));

        return builder.build();
    }

    static RadioOp toRadioOp(@NonNull Radio radio, @NonNull String containerKey) {
        RadioOp.Builder builder = RadioOp.newBuilder()
            .setWidgetProps(toWidgetProperties(radio, containerKey));

        if (radio.getOptions() != null) {
            for (String option : radio.getOptions()) {
                builder.addOptions(option);
            }
        }

        if (radio.getCaptions() != null) {
            for (String caption : radio.getCaptions()) {
                builder.addCaptions(caption);
            }
        }

        return builder.build();
    }

    static SelectboxOp toSelectboxOp(@NonNull io.streamlitconnect.widgets.Selectbox selectbox, @NonNull String containerKey) {
        SelectboxOp.Builder builder = SelectboxOp.newBuilder()
            .setWidgetProps(toWidgetProperties(selectbox, containerKey));

        if (selectbox.getOptions() != null) {
            for (String option : selectbox.getOptions()) {
                builder.addOptions(option);
            }
        }

        if (selectbox.getPlaceholder() != null) {
            builder.setPlaceholder(selectbox.getPlaceholder());
        }

        return builder.build();
    }

    static MultiselectOp toMultiselectOp(@NonNull Multiselect multiselect, @NonNull String containerKey) {
        MultiselectOp.Builder builder = MultiselectOp.newBuilder()
            .setWidgetProps(toWidgetProperties(multiselect, containerKey));

        if (multiselect.getOptions() != null) {
            for (String option : multiselect.getOptions()) {
                builder.addOptions(option);
            }
        }

        if (multiselect.getValue() != null) {
            for (int selectedIndex : multiselect.getValue()) {
                builder.addSelectedIndices(selectedIndex);
            }
        }

        if (multiselect.getPlaceholder() != null) {
            builder.setPlaceholder(multiselect.getPlaceholder());
        }

        return builder.build();
    }

    static SelectSliderOp toSelectSliderOp(@NonNull SelectSlider selectSlider, @NonNull String containerKey) {
        SelectSliderOp.Builder builder = SelectSliderOp.newBuilder()
            .setWidgetProps(toWidgetProperties(selectSlider, containerKey))
            .setLowerIndex(selectSlider.getValue().getLeft())
            .setUpperIndex(selectSlider.getValue().getRight());

        if (selectSlider.getOptions() != null) {
            for (String option : selectSlider.getOptions()) {
                builder.addOptions(option);
            }
        }

        return builder.build();
    }

    static DateInputOp toDateInputOp(@NonNull DateInput dateInput, @NonNull String containerKey) {
        DateInputOp.Builder builder = DateInputOp.newBuilder()
            .setWidgetProps(toWidgetProperties(dateInput, containerKey))
            .setDateFormatValue(dateInput.getFormat().ordinal())
            .setDateSeparatorValue(dateInput.getDateSeparator().ordinal());

        if (dateInput.getFromDate() != null) {
            builder.setFromDate(dateInput.getFromDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        if (dateInput.getToDate() != null) {
            builder.setToDate(dateInput.getToDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        if (dateInput.getMinDate() != null) {
            String minDate = dateInput.getMinDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            builder.setMinDate(minDate);
        }

        if (dateInput.getMaxDate() != null) {
            String maxDate = dateInput.getMaxDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            builder.setMaxDate(maxDate);
        }

        builder.setToday(
            dateInput.isToday() &&
                dateInput.getFromDate() == null &&
                dateInput.getToDate() == null
        );

        return builder.build();
    }

    static TimeInputOp toTimeInputOp(@NonNull TimeInput timeInput, @NonNull String containerKey) {
        TimeInputOp.Builder builder = TimeInputOp.newBuilder()
            .setWidgetProps(toWidgetProperties(timeInput, containerKey));

        if (timeInput.getValue() != null) {
            builder.setValue(timeInput.getValue().format(DateTimeFormatter.ISO_LOCAL_TIME));
        }

        builder.setStepSeconds(timeInput.getStepSeconds());

        return builder.build();
    }

    static NumberInputOp toNumberInputOp(@NonNull NumberInput numberInput, @NonNull String containerKey) {
        NumberInputOp.Builder builder = NumberInputOp.newBuilder()
            .setWidgetProps(toWidgetProperties(numberInput, containerKey));

        if (numberInput.getValue() != null) {
            builder.setValue(toIntOrFloat(numberInput.getValue()));
        }

        if (numberInput.getMin() != null) {
            builder.setMin(toIntOrFloat(numberInput.getMin()));
        }

        if (numberInput.getMax() != null) {
            builder.setMax(toIntOrFloat(numberInput.getMax()));
        }

        if (numberInput.getStep() != null) {
            builder.setStep(toIntOrFloat(numberInput.getStep()));
        }

        if (numberInput.getFormat() != null) {
            builder.setFormat(numberInput.getFormat());
        }

        if (numberInput.getPlaceholder() != null) {
            builder.setPlaceholder(numberInput.getPlaceholder());
        }

        return builder.build();
    }

    static TextInputOp toTextInputOp(@NonNull TextInput textInput, @NonNull String containerKey) {
        TextInputOp.Builder builder = TextInputOp.newBuilder()
            .setWidgetProps(toWidgetProperties(textInput, containerKey))
            .setTypeValue(textInput.getType().ordinal());

        if (textInput.getValue() != null) {
            builder.setValue(textInput.getValue());
        }

        if (textInput.getAutocomplete() != null) {
            builder.setAutocomplete(textInput.getAutocomplete());
        }

        if (textInput.getPlaceholder() != null) {
            builder.setPlaceholder(textInput.getPlaceholder());
        }

        return builder.build();
    }

    static SliderOp toSliderOp(@NonNull Slider<?, ?> slider, @NonNull String containerKey) {
        SliderOp.Builder builder = SliderOp.newBuilder()
            .setWidgetProps(toWidgetProperties(slider, containerKey));

        switch (slider) {

            case IntegerSlider integerSlider -> builder
                .setMinValueInt(integerSlider.getMin())
                .setMaxValueInt(integerSlider.getMax())
                .setStepInt(integerSlider.getStep())
                .setValueSingle(ValueSingle.newBuilder().setValueInt(integerSlider.getValue()).build());

            case FloatSlider floatSlider -> builder
                .setMinValueFloat(floatSlider.getMin())
                .setMaxValueFloat(floatSlider.getMax())
                .setStepFloat(floatSlider.getStep())
                .setValueSingle(ValueSingle.newBuilder().setValueFloat(floatSlider.getValue()).build());

            case DateSlider dateSlider -> builder
                .setMinValueDatetime(dateSlider.getMin().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .setMaxValueDatetime(dateSlider.getMax().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .setStepInt(dateSlider.getStep())
                .setValueSingle(
                    ValueSingle.newBuilder()
                        .setValueDatetime(dateSlider.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .build());

            case TimeSlider timeSlider -> builder
                .setMinValueDatetime(timeSlider.getMin().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .setMaxValueDatetime(timeSlider.getMax().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .setStepInt(timeSlider.getStep())
                .setValueSingle(
                    ValueSingle.newBuilder()
                        .setValueDatetime(timeSlider.getValue().format(DateTimeFormatter.ISO_LOCAL_TIME))
                        .build());

            default -> throw new StreamlitException("Unexpected value: " + slider);
        }

        if (slider.getFormat() != null) {
            builder.setFormat(slider.getFormat());
        }

        return builder.build();
    }

    static SliderOp toSliderOp(@NonNull RangeSlider<?, ?> slider, @NonNull String containerKey) {
        SliderOp.Builder builder = SliderOp.newBuilder()
            .setWidgetProps(toWidgetProperties(slider, containerKey));

        switch (slider) {

            case IntegerRangeSlider integerRangeSlider -> builder
                .setMinValueInt(integerRangeSlider.getMin())
                .setMaxValueInt(integerRangeSlider.getMax())
                .setStepInt(integerRangeSlider.getStep())
                .setValueRange(
                    ValueRange.newBuilder()
                        .setFromValueInt(integerRangeSlider.getValue().getLeft())
                        .setToValueInt(integerRangeSlider.getValue().getRight())
                        .build()
                );

            case FloatRangeSlider floatRangeSlider -> builder.setMinValueFloat(floatRangeSlider.getMin())
                .setMaxValueFloat(floatRangeSlider.getMax())
                .setStepFloat(floatRangeSlider.getStep())
                .setValueRange(
                    ValueRange.newBuilder()
                        .setFromValueFloat(floatRangeSlider.getValue().getLeft())
                        .setToValueFloat(floatRangeSlider.getValue().getRight())
                        .build()
                );

            case DateRangeSlider dateRangeSlider -> builder
                .setMinValueDatetime(dateRangeSlider.getMin().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .setMaxValueDatetime(dateRangeSlider.getMax().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .setStepInt(dateRangeSlider.getStep())
                .setValueRange(
                    ValueRange.newBuilder()
                        .setFromValueDatetime(dateRangeSlider.getValue().getLeft().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .setToValueDatetime(dateRangeSlider.getValue().getRight().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .build()
                );

            case TimeRangeSlider timeRangeSlider -> builder
                .setMinValueDatetime(timeRangeSlider.getMin().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .setMaxValueDatetime(timeRangeSlider.getMax().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .setStepInt(timeRangeSlider.getStep())
                .setValueRange(
                    ValueRange.newBuilder()
                        .setFromValueDatetime(timeRangeSlider.getValue().getLeft().format(DateTimeFormatter.ISO_LOCAL_TIME))
                        .setToValueDatetime(timeRangeSlider.getValue().getRight().format(DateTimeFormatter.ISO_LOCAL_TIME))
                        .build()
                );

            default -> throw new StreamlitException("Unexpected value: " + slider);
        }

        if (slider.getFormat() != null) {
            builder.setFormat(slider.getFormat());
        }

        return builder.build();
    }

    private static WidgetProperties toWidgetProperties(@NonNull Widget<?> widget, @NonNull String containerKey) {
        WidgetProperties.Builder builder = WidgetProperties.newBuilder()
            .setContainer(containerKey)
            .setKey(widget.getKey())
            .setLabel(widget.getLabel())
            .setDisabled(widget.isDisabled());

        if (widget.isUseContainerWidthSupported()) {
            builder.setUseContainerWidth(widget.isUseContainerWidth());
        }

        if (widget.isLabelVisibilitySupported()) {
            builder.setLabelVisibilityValue(widget.getLabelVisibility().ordinal());
        }

        if (widget.isHelpSupported() && StringUtils.isNotBlank(widget.getHelp())) {
            builder.setHelp(widget.getHelp());
        }

        return builder.build();
    }


    private static IntOrFloat toIntOrFloat(@NonNull Number value) {
        isTrue(value instanceof Integer || value instanceof Float, "Value must be an Integer or a Float");
        return value instanceof Integer
            ? IntOrFloat.newBuilder().setI((Integer) value).build()
            : IntOrFloat.newBuilder().setF((Float) value).build();
    }

}

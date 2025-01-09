package io.streamlitconnect.server.grpc;

import io.streamlitconnect.StreamlitException;
import io.streamlitconnect.StreamlitSessionContext;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.Action;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.ButtonAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.CheckboxAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.DateInputAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.MultiselectAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.NumberInputAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.RadioAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.SelectSliderAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.SelectboxAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.SliderAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.TextInputAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.TimeInputAction;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.ToggleAction;
import io.streamlitconnect.server.grpc.gen.StreamlitCommonsProto.IntOrFloat;
import io.streamlitconnect.widgets.Button;
import io.streamlitconnect.widgets.Checkbox;
import io.streamlitconnect.widgets.DateInput;
import io.streamlitconnect.widgets.DateSlider;
import io.streamlitconnect.widgets.FloatRangeSlider;
import io.streamlitconnect.widgets.FloatSlider;
import io.streamlitconnect.widgets.IntegerRangeSlider;
import io.streamlitconnect.widgets.IntegerSlider;
import io.streamlitconnect.widgets.Multiselect;
import io.streamlitconnect.widgets.NumberInput;
import io.streamlitconnect.widgets.Radio;
import io.streamlitconnect.widgets.SelectSlider;
import io.streamlitconnect.widgets.Selectbox;
import io.streamlitconnect.widgets.TextInput;
import io.streamlitconnect.widgets.TimeInput;
import io.streamlitconnect.widgets.TimeSlider;
import io.streamlitconnect.widgets.Toggle;
import io.streamlitconnect.widgets.Widget;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GrpcStreamlitSessionContext implements StreamlitSessionContext {

    private static final Logger log = LoggerFactory.getLogger(GrpcStreamlitSessionContext.class);

    @Getter
    private final String sessionId;

    @Getter
    private final String appName;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private Instant lastActivityAt;

    private final Map<String, Widget<?>> widgets = new HashMap<>();

    private final Map<String, Object> attributes = new HashMap<>();

    private int currentSeqNum;

    private GrpcOperationsRequestContext currentOpsReqContext;

    private GrpcNavigationRequestContext currentNavReqContext;

    final Lock lock = new ReentrantLock();

    final Condition requestFinished = lock.newCondition();

    GrpcStreamlitSessionContext(@NonNull String sessionId, String appName) {
        this.sessionId = sessionId;
        this.appName = appName;
    }

    @Override
    public Widget<?> getWidget(String widgetKey) {
        return widgets.get(widgetKey);
    }

    @Override
    public Object getAttribute(@NonNull String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(@NonNull String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void close() {
        attributes.clear();
        widgets.clear();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("sessionId", sessionId)
            .append("appName", appName)
            .append("lastActivityAt", lastActivityAt)
            .append("currentSeqNum", currentSeqNum)
            .append("currentNavReqContext", currentNavReqContext)
            .append("currentOpsReqContext", currentOpsReqContext)
            .append("widgets", widgets)
            .append("attributes", attributes)
            .toString();
    }

    int getCurrentSequenceNumber() {
        lock.lock();
        try {
            return currentSeqNum;
        } finally {
            lock.unlock();
        }
    }

    void setCurrentSequenceNumber(int sequenceNumber) {
        lock.lock();
        try {
            currentSeqNum = sequenceNumber;
            lastActivityAt = Instant.now();
        } finally {
            lock.unlock();
        }
    }

    //
    // Navigation request context management
    //

    GrpcNavigationRequestContext getCurrentNavReqContext() {
        lock.lock();
        try {
            return currentNavReqContext;
        } finally {
            lock.unlock();
        }
    }

    void setCurrentNavReqContext(@NonNull GrpcNavigationRequestContext navReqContext) {
        lock.lock();
        try {
            if (currentNavReqContext != null) {
                currentNavReqContext.cancel();
            }
            currentNavReqContext = navReqContext;
        } finally {
            lock.unlock();
        }
    }

    void waitForNavReqToFinish() {
        lock.lock();
        try {
            while (currentNavReqContext != null) {
                requestFinished.await();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for navigation request to finish", e);
            Thread.interrupted(); // clear interrupted status
            throw new StreamlitException("Interrupted while waiting for navigation request to finish", e);
        } finally {
            lock.unlock();
        }
    }

    void signalNavigationRequestFinished() {
        lock.lock();
        try {
            currentNavReqContext = null;
            requestFinished.signalAll();
        } finally {
            lock.unlock();
        }
    }

    //
    // Operations request context management
    //

    GrpcOperationsRequestContext getCurrentOpsReqContext() {
        lock.lock();
        try {
            return currentOpsReqContext;
        } finally {
            lock.unlock();
        }
    }

    void setCurrentOpsReqContext(@NonNull GrpcOperationsRequestContext opsReqContext) {
        lock.lock();
        try {
            if (currentOpsReqContext != null) {
                currentOpsReqContext.close();
            }
            currentOpsReqContext = opsReqContext;
        } finally {
            lock.unlock();
        }
    }

    void waitForOpsReqToFinish() {
        lock.lock();
        try {
            while (currentOpsReqContext != null) {
                requestFinished.await();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for operation request to finish", e);
            Thread.interrupted(); // clear interrupted status
            throw new StreamlitException("Interrupted while waiting for operation request to finish", e);
        } finally {
            lock.unlock();
        }
    }

    void signalOperationsRequestFinished() {
        lock.lock();
        try {
            currentOpsReqContext = null;
            requestFinished.signalAll();
        } finally {
            lock.unlock();
        }
    }

    //
    // Widget management
    //

    void addWidget(@NonNull Widget<?> widget) {
        widgets.put(widget.getKey(), widget);
    }

    void resetWidgets() {
        widgets.values().forEach(Widget::reset);
    }

    void resetAllWidgetsChangedFlags() {
        widgets.values().forEach(Widget::resetChanged);
    }

    //
    // Action processing
    //

    void processActions(List<Action> actions) {
        log.debug("Processing #actions: {}", actions.size());
        for (Action action : actions) {
            log.debug("Processing action: {}", action);

            switch (action.getActionCase()) {

                case BUTTON_ACTION:
                    handleButtonAction(action.getButtonAction());
                    break;

                case CHECKBOX_ACTION:
                    handleCheckboxAction(action.getCheckboxAction());
                    break;

                case TOGGLE_ACTION:
                    handleToggleAction(action.getToggleAction());
                    break;

                case RADIO_ACTION:
                    handleRadioAction(action.getRadioAction());
                    break;

                case SELECTBOX_ACTION:
                    handleSelectboxAction(action.getSelectboxAction());
                    break;

                case MULTISELECT_ACTION:
                    handleMultiselectAction(action.getMultiselectAction());
                    break;

                case SELECT_SLIDER_ACTION:
                    handleSelectSliderAction(action.getSelectSliderAction());
                    break;

                case DATE_INPUT_ACTION:
                    handleDateInputAction(action.getDateInputAction());
                    break;

                case TIME_INPUT_ACTION:
                    handleTimeInputAction(action.getTimeInputAction());
                    break;

                case NUMBER_INPUT_ACTION:
                    handleNumberInputAction(action.getNumberInputAction());
                    break;

                case TEXT_INPUT_ACTION:
                    handleTextInputAction(action.getTextInputAction());
                    break;

                case SLIDER_ACTION:
                    handleSliderAction(action.getSliderAction());
                    break;

                default:
                    throw new StreamlitException("Unsupported action: " + action.getActionCase());
            }
        }
    }

    private void handleButtonAction(ButtonAction action) {
        String key = action.getKey();
        Button button = (Button) getWidget(key);
        if (button != null) {
            List<String> args = action.getArgsList();
            Map<String, String> kwargs = action.getKwargsMap();
            button.setValue(null);
            button.onChange(args, kwargs);
            log.debug("Button clicked action: {}", button);
        } else {
            log.warn("Button not found for key: {}", key);
        }
    }

    private void handleCheckboxAction(CheckboxAction action) {
        String key = action.getKey();
        Checkbox checkbox = (Checkbox) getWidget(key);
        if (checkbox != null) {
            List<String> args = action.getArgsList();
            Map<String, String> kwargs = action.getKwargsMap();
            boolean changed = checkbox.setValue(action.getValue());
            if (changed) {
                log.debug(
                    "Checkbox '{}' changed from '{}' to '{}'",
                    key,
                    checkbox.getPreviousValue(),
                    checkbox.getValue());
                checkbox.onChange(args, kwargs);
            }
            log.debug("Checkbox action: {}", checkbox);
        } else {
            log.warn("Checkbox not found for key: {}", key);
        }
    }

    private void handleToggleAction(ToggleAction action) {
        String key = action.getKey();
        Toggle toggle = (Toggle) getWidget(key);
        if (toggle != null) {
            List<String> args = action.getArgsList();
            Map<String, String> kwargs = action.getKwargsMap();
            boolean changed = toggle.setValue(action.getValue());
            if (changed) {
                log.debug(
                    "Toggle '{}' changed from '{}' to '{}'",
                    key,
                    toggle.getPreviousValue(),
                    toggle.getValue());
                toggle.onChange(args, kwargs);
            }
            log.debug("Toggle action: {}", toggle);
        } else {
            log.warn("Toggle not found for key: {}", key);
        }
    }

    private void handleRadioAction(RadioAction radioAction) {
        String key = radioAction.getKey();
        Radio radio = (Radio) getWidget(key);
        if (radio != null) {
            List<String> args = radioAction.getArgsList();
            Map<String, String> kwargs = radioAction.getKwargsMap();
            boolean changed = radio.setValue(radioAction.getIndex());
            if (changed) {
                log.debug(
                    "Radio '{}' changed from '{}' to '{}'",
                    key,
                    radio.getPreviousValue(),
                    radio.getValue());
                radio.onChange(args, kwargs);
            }
            log.debug("Radio action: {}", radio);
        } else {
            log.warn("Radio not found for key: {}", key);
        }
    }

    private void handleSelectboxAction(SelectboxAction selectboxAction) {
        String key = selectboxAction.getKey();
        Selectbox selectbox = (Selectbox) getWidget(key);
        if (selectbox != null) {
            List<String> args = selectboxAction.getArgsList();
            Map<String, String> kwargs = selectboxAction.getKwargsMap();
            boolean changed = selectbox.setValue(selectboxAction.getIndex());
            if (changed) {
                log.debug(
                    "Selectbox '{}' changed from '{}' to '{}'",
                    key,
                    selectbox.getPreviousValue(),
                    selectbox.getValue());
                selectbox.onChange(args, kwargs);
            }
            log.debug("Selectbox action: {}", selectbox);
        } else {
            log.warn("Selectbox not found for key: {}", key);
        }
    }

    private void handleMultiselectAction(MultiselectAction multiselectAction) {
        String key = multiselectAction.getKey();
        Multiselect multiselect = (Multiselect) getWidget(key);
        if (multiselect != null) {
            int selectedIndicesCount = multiselectAction.getSelectedIndicesCount();
            int[] selectedIndices = new int[selectedIndicesCount];
            for (int i = 0; i < multiselectAction.getSelectedIndicesCount(); i++) {
                selectedIndices[i] = multiselectAction.getSelectedIndices(i);
            }

            boolean changed = multiselect.setValue(selectedIndices);
            if (changed) {
                log.debug(
                    "Multiselect '{}' changed from '{}' to '{}'",
                    key,
                    multiselect.getPreviousValue(),
                    multiselect.getValue());
                multiselect.onChange(multiselectAction.getArgsList(), multiselectAction.getKwargsMap());
            }
            log.debug("Multiselect action: {}", multiselect);
        } else {
            log.warn("Multiselect not found for key: {}", key);
        }
    }

    private void handleSelectSliderAction(SelectSliderAction selectSliderAction) {
        String key = selectSliderAction.getKey();
        SelectSlider selectSlider = (SelectSlider) getWidget(key);
        if (selectSlider != null) {
            boolean changed = selectSlider.setValue(
                MutablePair.of(selectSliderAction.getLowerIndex(), selectSliderAction.getUpperIndex()));
            if (changed) {
                log.debug(
                    "SelectSlider '{}' changed from '{}' to '{}'",
                    key,
                    selectSlider.getPreviousValue(),
                    selectSlider.getValue());
                selectSlider.onChange(selectSliderAction.getArgsList(), selectSliderAction.getKwargsMap());
            }
            log.debug("SelectSlider action: {}", selectSlider);
        } else {
            log.warn("SelectSlider not found for key: {}", key);
        }
    }

    private void handleDateInputAction(DateInputAction dateInputAction) {
        String key = dateInputAction.getKey();
        DateInput dateInput = (DateInput) getWidget(key);
        if (dateInput != null) {
            LocalDate actionFrom = null, actionTo = null;
            LocalDate existingFrom = dateInput.getValue().getLeft();
            LocalDate existingTo = dateInput.getValue().getRight();

            if (!dateInputAction.getFromDate().isEmpty()) {
                actionFrom = LocalDate.parse(dateInputAction.getFromDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            }

            if (!dateInputAction.getToDate().isEmpty()) {
                actionTo = LocalDate.parse(dateInputAction.getToDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            }

            LocalDate fromToUse = actionFrom != null ? actionFrom : existingFrom;
            LocalDate toToUse = actionTo != null ? actionTo : existingTo;

            boolean changed = dateInput.setValue(MutablePair.of(fromToUse, toToUse));
            if (changed) {
                log.debug("DateInput '{}' changed from '{}' to '{}'", key, dateInput.getPreviousValue(), dateInput.getValue());
                dateInput.onChange(dateInputAction.getArgsList(), dateInputAction.getKwargsMap());
            }
            log.debug("DateInput action: {}", dateInput);
        } else {
            log.warn("DateInput not found for key: {}", key);
        }
    }

    private void handleTimeInputAction(TimeInputAction timeInputAction) {
        String key = timeInputAction.getKey();
        TimeInput timeInput = (TimeInput) getWidget(key);
        if (timeInput != null) {
            boolean changed = false;

            if (!timeInputAction.getValue().isEmpty()) {
                LocalTime value = LocalTime.parse(timeInputAction.getValue(), DateTimeFormatter.ISO_LOCAL_TIME);
                changed = timeInput.setValue(value);
            }

            if (changed) {
                log.debug("TimeInput '{}' changed from '{}' to '{}'", key, timeInput.getPreviousValue(), timeInput.getValue());
                timeInput.onChange(timeInputAction.getArgsList(), timeInputAction.getKwargsMap());
            }
            log.debug("TimeInput action: {}", timeInput);
        } else {
            log.warn("TimeInput not found for key: {}", key);
        }
    }

    private void handleNumberInputAction(NumberInputAction numberInputAction) {
        String key = numberInputAction.getKey();
        NumberInput numberInput = (NumberInput) getWidget(key);
        if (numberInput != null) {
            Number newValue = getIntOrFloat(numberInputAction.getValue());
            boolean changed = numberInput.setValue(newValue);
            if (changed) {
                log.debug(
                    "NumberInput '{}' changed from '{}' to '{}'",
                    key,
                    numberInput.getPreviousValue(),
                    numberInput.getValue());
                numberInput.onChange(numberInputAction.getArgsList(), numberInputAction.getKwargsMap());
            }
            log.debug("NumberInput action: {}", numberInput);
        } else {
            log.warn("NumberInput not found for key: {}", key);
        }
    }

    private void handleTextInputAction(TextInputAction textInputAction) {
        String key = textInputAction.getKey();
        TextInput textInput = (TextInput) getWidget(key);
        if (textInput != null) {
            boolean changed = textInput.setValue(textInputAction.getValue());
            if (changed) {
                log.debug("TextInput '{}' changed from '{}' to '{}'", key, textInput.getPreviousValue(), textInput.getValue());
                textInput.onChange(textInputAction.getArgsList(), textInputAction.getKwargsMap());
            }
            log.debug("TextInput action: {}", textInput);
        } else {
            log.warn("TextInput not found for key: {}", key);
        }
    }

    private void handleSliderAction(SliderAction sliderAction) {
        String key = sliderAction.getKey();
        Widget<?> slider = getWidget(key);

        if (slider != null) {
            boolean changed =
                switch (slider) {

                    case IntegerSlider iSlider -> iSlider.setValue(sliderAction.getValueSingle().getValueInt());

                    case FloatSlider fSlider -> fSlider.setValue(sliderAction.getValueSingle().getValueFloat());

                    case DateSlider dateSlider -> dateSlider.setValue(
                        LocalDate.parse(sliderAction.getValueSingle().getValueDatetime()));

                    case TimeSlider timeSlider -> timeSlider.setValue(
                        LocalTime.parse(sliderAction.getValueSingle().getValueDatetime()));

                    case IntegerRangeSlider iRangeSlider -> iRangeSlider.setValue(
                        MutablePair.of(
                            sliderAction.getValueRange().getFromValueInt(),
                            sliderAction.getValueRange().getToValueInt()));

                    case FloatRangeSlider fRangeSlider -> fRangeSlider.setValue(
                        MutablePair.of(
                            sliderAction.getValueRange().getFromValueFloat(),
                            sliderAction.getValueRange().getToValueFloat()));

                    default -> throw new StreamlitException("Unsupported slider type: " + slider.getClass());
                };

            if (changed) {
                log.debug("{} '{}' changed from '{}' to '{}'",
                    slider.getClass().getSimpleName(),
                    key,
                    slider.getPreviousValue(),
                    slider.getValue());
                slider.onChange(sliderAction.getArgsList(), sliderAction.getKwargsMap());
            }
            log.debug("Slider action: {}", slider);

        } else {
            log.warn("Slider not found for key: {}", key);
        }
    }

    private static Number getIntOrFloat(IntOrFloat value) {
        if (value.hasI()) {
            return value.getI();
        } else if (value.hasF()) {
            return value.getF();
        }
        return null;
    }
}

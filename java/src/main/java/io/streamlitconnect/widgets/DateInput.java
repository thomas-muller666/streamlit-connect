package io.streamlitconnect.widgets;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 * A DateInput represents the interface for interacting with a corresponding Streamlit date input.
 * See <a href="https://docs.streamlit.io/develop/api-reference/widgets/st.date_input">Streamlit date input API doc</a>
 */
@Getter
@Setter
public class DateInput extends Widget<MutablePair<LocalDate, LocalDate>> {

    public enum Format {
        YYYYMMDD,
        DDMMYYYY,
        MMDDYYYY
    }

    public enum DateSeparator {
        SLASH,
        DASH,
        DOT
    }

    private Format format = Format.YYYYMMDD;

    private DateSeparator dateSeparator = DateSeparator.SLASH;

    private MutablePair<LocalDate, LocalDate> minMaxDate = MutablePair.of(null, null);

    private boolean today = true;

    public DateInput(@NonNull String label) {
        super(label, true, true, false, true);
    }

    @Override
    public void reset() {
        super.reset();
        value = MutablePair.of(null, null);
        previousValue = MutablePair.of(null, null);
    }

    public LocalDate getFromDate() {
        return value.getLeft();
    }

    public LocalDate getToDate() {
        return value.getRight();
    }

    public LocalDate getMinDate() {
        return minMaxDate.getLeft();
    }

    public LocalDate getMaxDate() {
        return minMaxDate.getRight();
    }

    public void setFromDateToToday() {
        value.setLeft(LocalDate.now());
    }

    public void setFromDateToTomorrow() {
        value.setLeft(LocalDate.now().plusDays(1));
    }

    public void setToDateToToday() {
        value.setRight(LocalDate.now());
    }

    public void setToDateToYesterday() {
        value.setRight(LocalDate.now().minusDays(1));
    }

    public void setMinDateToToday() {
        minMaxDate.setLeft(LocalDate.now());
    }

    public void setMinDateToTomorrow() {
        minMaxDate.setLeft(LocalDate.now().plusDays(1));
    }

    public void setMaxDateToToday() {
        minMaxDate.setRight(LocalDate.now());
    }

    public void setMaxDateToYesterday() {
        minMaxDate.setRight(LocalDate.now().minusDays(1));
    }

    public boolean isValid() {
        LocalDate from = value.getLeft();
        LocalDate to = value.getRight();
        LocalDate min = minMaxDate.getLeft();
        LocalDate max = minMaxDate.getRight();

        return (from == null || to == null || from.isBefore(to)) &&
            (min == null || max == null || min.isBefore(max));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("fromDate", value.getLeft())
            .append("toDate", value.getRight())
            .append("minDate", minMaxDate.getLeft())
            .append("maxDate", minMaxDate.getRight())
            .append("today", today)
            .toString();
    }

}

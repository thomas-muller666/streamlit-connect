package io.streamlitconnect;

import static org.apache.commons.lang3.Validate.isTrue;

import io.streamlitconnect.ColumnContainer.ColumnGap;
import io.streamlitconnect.ColumnContainer.ColumnVerticalAlignment;
import io.streamlitconnect.widgets.Widget;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * A Container represents the interface for interacting with a corresponding Streamlit container.
 */
public interface Container {

    /**
     * Returns the parent container of this container, or null if this container is the root container.
     *
     * @return The parent container.
     */
    Container parent();

    /**
     * Returns the children containers of this container.
     *
     * @return The children containers.
     */
    List<Container> children();

    /**
     * Sends a rerun message to the Streamlit front-end.
     *
     * @return The Streamlit container id.
     */
    void rerun();

    /**
     * Premature stop of the Streamlit application.
     */
    default void stop() {
        stop(null);
    }

    /**
     * Premature stop of the Streamlit application with a message.
     *
     * @param message The message to set.
     */
    void stop(String message);

    /**
     * Switches to the given page in the Streamlit application.
     *
     * @param pageName The page name to set.
     */
    void switchPage(@NonNull String pageName);

    default Container title(@NonNull String title) {
        return title(title, null);
    }

    default Container title(@NonNull String title, String anchor) {
        return title(title, anchor, null);
    }

    /**
     * Outputs a title in the Streamlit container.
     * <p>
     * See corresponding
     * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#title">Streamlit title API doc</a>}
     *
     * @param title  The title to set.
     * @param anchor The anchor to set.
     * @param help   The help to set.
     * @return This container.
     */
    Container title(@NonNull String title, String anchor, String help);

    default Container header(@NonNull String header) {
        return header(header, null);
    }

    default Container header(@NonNull String header, String anchor) {
        return header(header, anchor, null, false);
    }

    default Container header(@NonNull String header, String anchor, String help) {
        return header(header, anchor, help, false);
    }

    /**
     * Outputs a header in the Streamlit container.
     * <p>
     * See corresponding
     * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#header">Streamlit header API
     * doc</a>}
     *
     * @param header The header to set.
     * @param anchor The anchor to set.
     * @param help   The help to set.
     * @param divider Whether to show a divider.
     * @return This container.
     */
    Container header(@NonNull String header, String anchor, String help, boolean divider);

    default Container subheader(@NonNull String subheader) {
        return subheader(subheader, null);
    }

    default Container subheader(@NonNull String subheader, String anchor) {
        return subheader(subheader, anchor, null);
    }

    default Container subheader(@NonNull String subheader, String anchor, String help) {
        return subheader(subheader, anchor, help, false);
    }

    /**
     * Outputs a subheader in the Streamlit container.
     * <p>
     * See corresponding
     * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#subheader"> Streamlit subheader API
     * doc</a>}
     *
     * @param subheader The subheader to set.
     * @param anchor    The anchor to set.
     * @param help      The help to set.
     * @param divider Whether to show a divider.
     * @return This container.
     */
    Container subheader(@NonNull String subheader, String anchor, String help, boolean divider);

    default Container text(@NonNull String text) {
        return text(text, null);
    }

    /**
     * Outputs text in the Streamlit container.
     * <p>
     * See corresponding
     * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#text">Streamlit text API doc</a>}
     *
     * @param text The text to set.
     * @param help The help to set.
     */
    Container text(@NonNull String text, String help);

    default Container markdown(@NonNull String markdown) {
        return markdown(markdown, false);
    }

    default Container markdown(@NonNull String markdown, boolean unsafeAllowHtml) {
        return markdown(markdown, unsafeAllowHtml, null);
    }

    /**
     * Outputs markdown in the Streamlit container.
     * <p>
     * See corresponding
     * {@link <a href="https://docs.streamlit.io/library/api-reference/widgets/api-reference#markdown"> Streamlit markdown API
     * doc</a>}
     *
     * @param markdown        The markdown to set.
     * @param unsafeAllowHtml The unsafeAllowHtml to set.
     * @param help            The help to set.
     */
    Container markdown(@NonNull String markdown, boolean unsafeAllowHtml, String help);

    /**
     * Writes the given chunks to the Streamlit container as a stream. This method is non-blocking and will return immediately.
     *
     * @param chunks The chunks to write.
     */
    void writeStream(Iterator<String> chunks);

    /**
     * Outputs a widget in the Streamlit container. See {@link io.streamlitconnect.widgets} for available widgets, each with a
     * corresponding Streamlit widget.
     *
     * @param widget The widget to set.
     */
    Container widget(@NonNull Widget<?> widget);

    default Container innerContainer() {
        return innerContainer(0, false);
    }

    default Container innerContainer(boolean border) {
        return innerContainer(0, border);
    }

    /**
     * Returns a nested container in the Streamlit container. See
     * {@link <a href="https://docs.streamlit.io/develop/api-reference/layout/st.container"> Streamlit st.container API doc</a>}
     *
     * @param height The height (in pixels) to set - <= 0 means auto height.
     * @param border Whether to show a border around the container.
     * @return The nested container.
     */
    Container innerContainer(int height, boolean border);

    default Container expander(String label) {
        return expander(label, null, false);
    }

    /**
     * Returns an expandable container in the Streamlit container. See
     * {@link <a href="https://docs.streamlit.io/develop/api-reference/layout/st.expander"> Streamlit st.expander API doc</a>}
     *
     * @return The expandable container.
     */
    Container expander(String label, String icon, boolean initiallyExpanded);

    /**
     * Returns a map of tab containers in the Streamlit container. See
     * {@link <a href="https://docs.streamlit.io/develop/api-reference/layout/st.tabs"> Streamlit st.tabs API doc</a>}
     *
     * @param names The names of the tabs.
     * @return A map tab containers where the key is the tab name.
     */
    Map<String, Container> tabs(@NonNull String... names);

    default List<Container> columns(int numColumns) {
        return columns(numColumns, ColumnGap.SMALL, ColumnVerticalAlignment.TOP);
    }

    default List<Container> columns(int numColumns, @NonNull ColumnContainer.ColumnGap gap, @NonNull ColumnVerticalAlignment verticalAlignment) {
        isTrue(numColumns > 0, "Number of columns must be greater than 0");

        // Create a list of equal width columns
        float[] columnWidths = new float[numColumns];
        Arrays.fill(columnWidths, 1.0f / numColumns);  // Each column should have equal width i.e., 1/numColumns

        // Finally, call the original method with the computed columns widths
        return columns(gap, verticalAlignment, columnWidths);
    }

    default List<Container> proportionalColumns(
        @NonNull List<Float> proportionalWidthsList,
        @NonNull ColumnContainer.ColumnGap gap,
        @NonNull ColumnVerticalAlignment verticalAlignment
    ) {
        isTrue(!proportionalWidthsList.isEmpty(), "List must not be null or empty");

        // Create an array of widths directly from proportionalWidthsList
        float[] columnWidths = new float[proportionalWidthsList.size()];
        for (int i = 0; i < proportionalWidthsList.size(); i++) {
            columnWidths[i] = proportionalWidthsList.get(i);
        }

        return columns(gap, verticalAlignment, columnWidths);
    }

    /**
     * Returns a list of column containers in the Streamlit container. See
     * {@link <a href="https://docs.streamlit.io/develop/api-reference/layout/st.columns"> Streamlit st.columns API doc</a>}
     *
     * @param gap               The gap to set.
     * @param verticalAlignment The verticalAlignment to set.
     * @param columnWidths      The columnWidths to set.
     * @return The column containers.
     */
    List<Container> columns(@NonNull ColumnContainer.ColumnGap gap, @NonNull ColumnVerticalAlignment verticalAlignment, float... columnWidths);

    /**
     * Returns an empty (placeholder) container in the Streamlit container. See
     * <a href="https://docs.streamlit.io/develop/api-reference/layout/st.empty">Streamlit st.empty API doc</a>
     * If the calling container is an empty container, it will be emptied and the same container will be returned.
     *
     * @return The empty container - either a new container or the same container.
     */
    Container placeholder();

}

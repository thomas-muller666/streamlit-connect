package io.streamlitconnect.server.grpc;

import static org.apache.commons.lang3.Validate.isTrue;

import io.streamlitconnect.ColumnContainer;
import io.streamlitconnect.Container;
import io.streamlitconnect.PlaceholderContainer;
import io.streamlitconnect.RootContainer;
import io.streamlitconnect.SidebarContainer;
import io.streamlitconnect.StreamlitException;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.ColumnContainersOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.ExpandableContainerOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.HeaderOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.InnerContainerOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.MarkdownOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.PlaceholderContainerOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.RerunOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.StopOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.StreamlitOperation;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.SubheaderOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.SwitchPageOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.TabContainersOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.TextOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.TitleOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.WriteStreamChunkOp;
import io.streamlitconnect.widgets.Button;
import io.streamlitconnect.widgets.Checkbox;
import io.streamlitconnect.widgets.DateInput;
import io.streamlitconnect.widgets.Multiselect;
import io.streamlitconnect.widgets.NumberInput;
import io.streamlitconnect.widgets.PageLink;
import io.streamlitconnect.widgets.Radio;
import io.streamlitconnect.widgets.RangeSlider;
import io.streamlitconnect.widgets.SelectSlider;
import io.streamlitconnect.widgets.Selectbox;
import io.streamlitconnect.widgets.Slider;
import io.streamlitconnect.widgets.TextInput;
import io.streamlitconnect.widgets.TimeInput;
import io.streamlitconnect.widgets.Toggle;
import io.streamlitconnect.widgets.Widget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContainerImpl implements Container {

    protected static final Logger log = LoggerFactory.getLogger(ContainerImpl.class);

    // Unique key for this container (e.g. "root", "sidebar", "tab1", etc.)
    @Getter(AccessLevel.PACKAGE)
    private final String key;

    @Getter(AccessLevel.PROTECTED)
    private final ContainerImpl parent;

    @Getter(AccessLevel.PROTECTED)
    private final List<Container> children = new ArrayList<>();

    @Getter(AccessLevel.PROTECTED)
    private final GrpcOperationsRequestContext context;

    ContainerImpl(
        @NonNull String key,
        ContainerImpl parent,
        GrpcOperationsRequestContext context
    ) {
        this.key = key;
        this.parent = parent;
        this.context = context;

        // Check if the parent is null that we're a root container or sidebar container
        if (parent == null && !(this instanceof RootContainer || this instanceof SidebarContainer)) {
            throw new StreamlitException("Root or sidebar container cannot have a parent.");
        }

        if (parent != null) {
            parent.addChild(this);
        }
    }

    void addChild(@NonNull ContainerImpl child) {
        if (child.parent() != this) {
            throw new StreamlitException("Child parent mismatch. Expected: " + this + ", actual: " + child.parent());
        }
        children.add(child);
    }

    @Override
    public Container parent() {
        return parent;
    }

    @Override
    public List<Container> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void rerun() {
        checkCancelled();
        RerunOp.Builder builder = RerunOp.newBuilder();
        StreamlitOperation op = StreamlitOperation.newBuilder().setRerunOp(builder.build()).build();
        log.debug("Queuing Rerun operation: {}", op);
        context.enqueueOp(op);
    }

    @Override
    public void stop(String message) {
        checkCancelled();
        StopOp.Builder builder = StopOp.newBuilder();

        if (message != null) {
            builder.setMessage(message);
        }

        StreamlitOperation op = StreamlitOperation.newBuilder().setStopOp(builder.build()).build();
        log.debug("Queuing Stop operation: {}", op);
        context.enqueueOp(op);
    }

    @Override
    public Container title(@NonNull String title, String anchor, String help) {
        checkCancelled();
        TitleOp.Builder builder = TitleOp.newBuilder()
            .setContainer(key)
            .setBody(title);

        if (anchor != null) {
            builder.setAnchor(anchor);
        }

        if (help != null) {
            builder.setHelp(help);
        }

        StreamlitOperation op = StreamlitOperation.newBuilder().setTitleOp(builder.build()).build();
        log.debug("Queuing Title operation: {}", op);
        context.enqueueOp(op);
        return this;
    }

    @Override
    public Container header(@NonNull String header, String anchor, String help, boolean divider) {
        checkCancelled();
        HeaderOp.Builder builder = HeaderOp.newBuilder()
            .setContainer(key)
            .setBody(header)
            .setDivider(divider);

        if (anchor != null) {
            builder.setAnchor(anchor);
        }

        if (help != null) {
            builder.setHelp(help);
        }

        StreamlitOperation op = StreamlitOperation.newBuilder().setHeaderOp(builder.build()).build();
        log.debug("Queuing Header operation: {}", op);
        context.enqueueOp(op);
        return this;
    }

    @Override
    public Container subheader(@NonNull String subheader, String anchor, String help, boolean divider) {
        checkCancelled();
        SubheaderOp.Builder builder = SubheaderOp.newBuilder()
            .setContainer(key)
            .setBody(subheader)
            .setDivider(divider);

        if (anchor != null) {
            builder.setAnchor(anchor);
        }

        if (help != null) {
            builder.setHelp(help);
        }
        StreamlitOperation op = StreamlitOperation.newBuilder().setSubheaderOp(builder.build()).build();
        log.debug("Queuing Subheader operation: {}", op);
        context.enqueueOp(op);
        return this;
    }

    @Override
    public Container text(@NonNull String text, String help) {
        checkCancelled();
        TextOp.Builder builder = TextOp.newBuilder()
            .setContainer(key)
            .setBody(text);

        if (help != null) {
            builder.setHelp(help);
        }

        StreamlitOperation op = StreamlitOperation.newBuilder().setTextOp(builder.build()).build();
        log.debug("Queuing Text operation: {}", op);
        context.enqueueOp(op);
        return this;
    }

    @Override
    public Container markdown(@NonNull String markdown, boolean unsafeAllowHtml, String help) {
        checkCancelled();
        MarkdownOp.Builder builder = MarkdownOp.newBuilder()
            .setContainer(key)
            .setBody(markdown)
            .setUnsafeAllowHtml(unsafeAllowHtml);

        if (help != null) {
            builder.setHelp(help);
        }
        StreamlitOperation op = StreamlitOperation.newBuilder().setMarkdownOp(builder.build()).build();
        log.debug("Queuing Markdown operation: {}", op);
        context.enqueueOp(op);
        return this;
    }

    @Override
    public void writeStream(Iterator<String> chunks) {
        checkCancelled();
        String streamKey = this.key + "(stream_" + Utils.randomKeySuffix() + ")";

        // Post WriteStreamChunk operations
        Runnable task = () -> {
            boolean isFirstChunk = true;
            while (chunks.hasNext()) {
                String chunk = chunks.next();

                WriteStreamChunkOp.Builder builder = WriteStreamChunkOp.newBuilder();

                if (isFirstChunk) {
                    builder.setContainer(getKey());
                    isFirstChunk = false;
                }

                builder.setKey(streamKey).setBody(chunk);

                if (!chunks.hasNext()) {
                    builder.setIsLast(true);
                }

                StreamlitOperation op = StreamlitOperation.newBuilder().setWriteStreamChunkOp(builder.build()).build();
                log.debug("Queuing WriteStreamChunk operation: {}", op);
                context.enqueueOp(op);
            }
            log.debug("Finished writing chunks for stream: {}", streamKey);
        };

        context.execute(task);
    }

    @Override
    public void switchPage(@NonNull String pageName) {
        checkCancelled();
        SwitchPageOp.Builder builder = SwitchPageOp.newBuilder()
            .setPage(pageName);

        StreamlitOperation op = StreamlitOperation.newBuilder().setSwitchPageOp(builder.build()).build();
        log.debug("Queuing SwitchPage operation: {}", op);
        context.enqueueOp(op);
    }

    @Override
    public Container innerContainer(int height, boolean border) {
        checkCancelled();
        InnerContainerImpl innerContainer = new InnerContainerImpl(
            this,
            context,
            height,
            border
        );

        InnerContainerOp.Builder builder = InnerContainerOp.newBuilder()
            .setParent(key)
            .setKey(innerContainer.getKey())
            .setHeight(height)
            .setBorder(border);

        StreamlitOperation op = StreamlitOperation.newBuilder().setInnerContainerOp(builder.build()).build();
        log.debug("Queuing InnerContainer operation: {}", op);
        context.enqueueOp(op);
        return innerContainer;
    }

    @Override
    public Container expander(String label, String icon, boolean initiallyExpanded) {
        checkCancelled();
        ExpandableContainerImpl expandableContainer = new ExpandableContainerImpl(
            this,
            context,
            label,
            initiallyExpanded,
            icon
        );

        postExpandableContainerMsg(label, icon, initiallyExpanded, expandableContainer);
        return expandableContainer;
    }

    private void postExpandableContainerMsg(
        String label,
        String icon,
        boolean initiallyExpanded,
        ExpandableContainerImpl expandableContainer
    ) {
        checkCancelled();
        ExpandableContainerOp.Builder builder = ExpandableContainerOp.newBuilder()
            .setParent(key)
            .setKey(expandableContainer.getKey())
            .setExpanded(initiallyExpanded);

        if (label != null) {
            builder.setLabel(label);
        }

        if (icon != null) {
            builder.setIcon(icon);
        }

        StreamlitOperation op = StreamlitOperation.newBuilder().setExpandableContainerOp(builder.build()).build();
        log.debug("Queuing ExpandableContainer operation: {}", op);
        context.enqueueOp(op);
    }

    @Override
    public Map<String, Container> tabs(@NonNull String... names) {
        isTrue(names.length > 0, "Number of tabs must be greater than 0");
        checkCancelled();

        Map<String, Container> containers = new LinkedHashMap<>(names.length + 1);
        List<TabContainerImpl> tabs = new ArrayList<>(names.length + 1);

        for (String name : names) {
            // Check if name is null or empty
            isTrue(name != null && !name.isEmpty(), "Tab name cannot be null or empty");

            TabContainerImpl tabContainer = new TabContainerImpl(this, context, name);
            containers.put(name, tabContainer);
            tabs.add(tabContainer);
        }

        postTabContainersOp(tabs);

        return Collections.unmodifiableMap(containers);
    }

    private void postTabContainersOp(@NotNull List<TabContainerImpl> tabs) {
        TabContainersOp.Builder builder = TabContainersOp.newBuilder()
            .setParent(key);

        for (TabContainerImpl tab : tabs) {
            builder.addKeys(tab.getKey());
            builder.addTabs(tab.getName());
        }

        StreamlitOperation op = StreamlitOperation.newBuilder().setTabContainersOp(builder.build()).build();
        log.debug("Queuing TabContainers operation: {}", op);
        context.enqueueOp(op);
    }

    @Override
    public List<Container> columns(
        @NonNull ColumnContainer.ColumnGap gap,
        @NonNull ColumnContainer.ColumnVerticalAlignment verticalAlignment,
        float... columnWidths // 0.0 to 1.0 with sum <= 1.0
    ) {
        // Check that the size of columnWidths is > 0
        isTrue(columnWidths.length > 0, "Number of columns must be greater than 0");

        checkCancelled();

        // Check that the sum of columnWidths is <= 1.0
        float sum = 0.0f;
        for (float columnWidth : columnWidths) {
            sum += columnWidth;
        }
        isTrue(sum <= 1.0f, "Sum of column widths must be <= 1.0");

        List<Container> containers = new ArrayList<>(columnWidths.length + 1);

        for (int i = 0; i < columnWidths.length; i++) {
            ColumnContainerImpl columnContainer = new ColumnContainerImpl(
                this,
                context,
                i,
                columnWidths[i]
            );
            containers.add(columnContainer);
        }

        postColumnContainersOp(containers, gap, verticalAlignment);

        return Collections.unmodifiableList(containers);
    }

    private void postColumnContainersOp(
        List<Container> containers,
        @NonNull ColumnContainer.ColumnGap colGap,
        @NonNull ColumnContainer.ColumnVerticalAlignment verticalAlignment
    ) {
        ColumnContainersOp.Gap gap = switch (colGap) {
            case SMALL -> ColumnContainersOp.Gap.SMALL;
            case MEDIUM -> ColumnContainersOp.Gap.MEDIUM;
            case LARGE -> ColumnContainersOp.Gap.LARGE;
        };

        ColumnContainersOp.VerticalAlignment verAl = switch (verticalAlignment) {
            case TOP -> ColumnContainersOp.VerticalAlignment.TOP;
            case CENTER -> ColumnContainersOp.VerticalAlignment.CENTER;
            case BOTTOM -> ColumnContainersOp.VerticalAlignment.BOTTOM;
        };

        ColumnContainersOp.Builder builder = ColumnContainersOp.newBuilder()
            .setParent(key)
            .setGap(gap)
            .setVerticalAlignment(verAl);

        for (Container container : containers) {
            ColumnContainerImpl colContainer = (ColumnContainerImpl) container;
            builder.addKeys(colContainer.getKey());
            builder.addWidths(colContainer.getWidth());
        }

        StreamlitOperation op = StreamlitOperation.newBuilder().setColumnContainersOp(builder.build()).build();
        log.debug("Queuing ColumnContainer operation: {}", op);
        context.enqueueOp(op);
    }

    @Override
    public Container placeholder() {
        checkCancelled();

        if (parent instanceof PlaceholderContainer placeholderContainer) {
            placeholderContainer.empty();
            return this;
        }

        PlaceholderContainerImpl placeholderContainer = new PlaceholderContainerImpl(this, context);

        postPlaceholderContainerOp(placeholderContainer);

        return placeholderContainer;
    }

    private void postPlaceholderContainerOp(PlaceholderContainerImpl placeholderContainer) {
        PlaceholderContainerOp.Builder builder = PlaceholderContainerOp.newBuilder()
            .setParent(key)
            .setKey(placeholderContainer.getKey());

        StreamlitOperation op = StreamlitOperation.newBuilder().setPlaceholderContainerOp(builder.build()).build();
        log.debug("Queuing PlaceholderContainer operation: {}", op);
        context.enqueueOp(op);
    }

    @Override
    public Container widget(@NonNull Widget<?> widget) {
        checkCancelled();

        switch (widget) {
            case Button button -> button(button);
            case Checkbox checkbox -> checkbox(checkbox);
            case Toggle toggle -> toggle(toggle);
            case Radio radio -> radio(radio);
            case Selectbox selectbox -> selectbox(selectbox);
            case Multiselect multiselect -> multiselect(multiselect);
            case SelectSlider selectSlider -> selectSlider(selectSlider);
            case DateInput dateInput -> dateInput(dateInput);
            case TimeInput timeInput -> timeInput(timeInput);
            case PageLink pageLink -> pageLink(pageLink);
            case NumberInput numberInput -> numberInput(numberInput);
            case TextInput textInput -> textInput(textInput);
            case Slider<?, ?> slider -> slider(slider);
            case RangeSlider<?, ?> rangeSlider -> rangeSlider(rangeSlider);
            default -> throw new StreamlitException("Unsupported widget: " + widget.getClass().getName());
        }
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("key", key)
            .append("parent.key", parent.getKey())
            .append("children.keys", children.stream()
                .map(child -> ((ContainerImpl) child).getKey())
                .toList())
            .toString();
    }

    private void checkCancelled() {
        if (context.isCancelled()) {
            throw new StreamlitException("Request context cancelled: " + context);
        }
    }

    private void button(@NonNull Button button) {
        context.getSessionContext().addWidget(button);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setButtonOp(WidgetMapper.toButtonOp(button, key))
            .build();
        log.debug("Queuing Button operation: {}", op);
        context.enqueueOp(op);
    }

    private void pageLink(@NonNull PageLink pageLink) {
        context.getSessionContext().addWidget(pageLink);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setPageLinkOp(WidgetMapper.toPageLinkOp(pageLink, key))
            .build();
        log.debug("Queuing PageLink operation: {}", op);
        context.enqueueOp(op);
    }

    private void checkbox(@NonNull Checkbox checkbox) {
        context.getSessionContext().addWidget(checkbox);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setCheckboxOp(WidgetMapper.toCheckboxOp(checkbox, key))
            .build();
        log.debug("Queuing Checkbox operation: {}", op);
        context.enqueueOp(op);
    }

    private void toggle(@NonNull Toggle toggle) {
        context.getSessionContext().addWidget(toggle);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setToggleOp(WidgetMapper.toToggleOp(toggle, key))
            .build();
        log.debug("Queuing Toggle operation: {}", op);
        context.enqueueOp(op);
    }

    private void radio(@NonNull Radio radio) {
        context.getSessionContext().addWidget(radio);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setRadioOp(WidgetMapper.toRadioOp(radio, key))
            .build();
        log.debug("Queuing Radio operation: {}", op);
        context.enqueueOp(op);
    }

    private void selectbox(@NonNull Selectbox selectbox) {
        context.getSessionContext().addWidget(selectbox);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setSelectboxOp(WidgetMapper.toSelectboxOp(selectbox, key))
            .build();
        log.debug("Queuing Selectbox operation: {}", op);
        context.enqueueOp(op);
    }

    private void multiselect(@NonNull Multiselect multiselect) {
        context.getSessionContext().addWidget(multiselect);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setMultiselectOp(WidgetMapper.toMultiselectOp(multiselect, key))
            .build();
        log.debug("Queuing Multiselect operation: {}", op);
        context.enqueueOp(op);
    }

    private void selectSlider(@NonNull SelectSlider selectSlider) {
        context.getSessionContext().addWidget(selectSlider);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setSelectSliderOp(WidgetMapper.toSelectSliderOp(selectSlider, key))
            .build();
        log.debug("Queuing SelectSlider operation: {}", op);
        context.enqueueOp(op);
    }

    private void dateInput(@NonNull DateInput dateInput) {
        if (!dateInput.isValid()) {
            throw new StreamlitException("Invalid DateInput: " + dateInput);
        }
        context.getSessionContext().addWidget(dateInput);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setDateInputOp(WidgetMapper.toDateInputOp(dateInput, key))
            .build();
        log.debug("Queuing DateInput operation: {}", op);
        context.enqueueOp(op);
    }

    private void timeInput(@NonNull TimeInput timeInput) {
        context.getSessionContext().addWidget(timeInput);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setTimeInputOp(WidgetMapper.toTimeInputOp(timeInput, key))
            .build();
        log.debug("Queuing TimeInput operation: {}", op);
        context.enqueueOp(op);
    }

    private void numberInput(@NonNull NumberInput numberInput) {
        context.getSessionContext().addWidget(numberInput);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setNumberInputOp(WidgetMapper.toNumberInputOp(numberInput, key))
            .build();
        log.debug("Queuing NumberInput operation: {}", op);
        context.enqueueOp(op);
    }

    private void textInput(@NonNull TextInput textInput) {
        context.getSessionContext().addWidget(textInput);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setTextInputOp(WidgetMapper.toTextInputOp(textInput, key))
            .build();
        log.debug("Queuing TextInput operation: {}", op);
        context.enqueueOp(op);
    }

    private void slider(@NonNull Slider<?, ?> slider) {
        context.getSessionContext().addWidget(slider);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setSliderOp(WidgetMapper.toSliderOp(slider, key))
            .build();
        log.debug("Queuing Slider operation: {}", op);
        context.enqueueOp(op);
    }

    private void rangeSlider(RangeSlider<?, ?> rangeSlider) {
        context.getSessionContext().addWidget(rangeSlider);
        StreamlitOperation op = StreamlitOperation.newBuilder()
            .setSliderOp(WidgetMapper.toSliderOp(rangeSlider, key))
            .build();
        log.debug("Queuing RangeSlider operation: {}", op);
        context.enqueueOp(op);
    }
}

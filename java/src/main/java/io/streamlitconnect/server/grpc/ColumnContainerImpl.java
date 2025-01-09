package io.streamlitconnect.server.grpc;

import static org.apache.commons.lang3.Validate.isTrue;

import io.streamlitconnect.ColumnContainer;
import io.streamlitconnect.Container;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Getter
class ColumnContainerImpl extends ContainerImpl implements ColumnContainer {

    private final int column; // 1-indexed column number

    private final float width;

    ColumnContainerImpl(
        ContainerImpl parent,
        GrpcOperationsRequestContext context,
        int col,
        float width
    ) {
        super("column_" + Utils.randomKeySuffix() + "(" + col + ")", parent, context);
        isTrue(col >= 0, "Column number must be greater than 0");

        for (Container container = parent; container != null; container = container.parent()) {
            if (container instanceof ColumnContainer) {
                throw new IllegalArgumentException("ColumnContainer cannot be nested inside another ColumnContainer");
            }
        }

        this.column = col;
        this.width = width;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .appendSuper(super.toString())
            .append("column", column)
            .append("width", width)
            .toString();
    }

}

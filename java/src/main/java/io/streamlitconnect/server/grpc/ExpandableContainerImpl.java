package io.streamlitconnect.server.grpc;

import io.streamlitconnect.Container;
import io.streamlitconnect.ExpandableContainer;
import lombok.Getter;

@Getter
class ExpandableContainerImpl extends ContainerImpl implements ExpandableContainer {

    private final String label;

    private final String icon;

    private final boolean initiallyExpanded;

    ExpandableContainerImpl(
        ContainerImpl parent,
        GrpcOperationsRequestContext context,
        String label,
        boolean initiallyExpanded,
        String icon
    ) {
        super("expandable_" + Utils.randomKeySuffix(), parent, context);

        // Check that none of the parent containers are expandable
        for (Container container = parent; container != null; container = container.parent()) {
            if (container instanceof ExpandableContainer) {
                throw new IllegalArgumentException("ExpandableContainer cannot be nested inside another ExpandableContainer");
            }
        }
        this.label = label;
        this.initiallyExpanded = initiallyExpanded;
        this.icon = icon;
    }

}

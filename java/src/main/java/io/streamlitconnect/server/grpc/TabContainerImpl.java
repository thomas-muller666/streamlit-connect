package io.streamlitconnect.server.grpc;

import io.streamlitconnect.TabContainer;
import lombok.Getter;
import lombok.NonNull;

@Getter
class TabContainerImpl extends ContainerImpl implements TabContainer {

    private final String name; // The name of the tab is it will appear in the UI

    TabContainerImpl(
        @NonNull ContainerImpl parent,
        GrpcOperationsRequestContext context,
        @NonNull String name
    ) {
        super("tab_" + Utils.randomKeySuffix() + "(" + name + ")", parent, context);
        this.name = name;
    }

}

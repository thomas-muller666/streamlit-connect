package io.streamlitconnect.server.grpc;

import io.streamlitconnect.SidebarContainer;

class SidebarContainerImpl extends ContainerImpl implements SidebarContainer {

    public static final String KEY = "sidebar";

    SidebarContainerImpl(GrpcOperationsRequestContext context) {
        super(KEY, null, context);
    }


}

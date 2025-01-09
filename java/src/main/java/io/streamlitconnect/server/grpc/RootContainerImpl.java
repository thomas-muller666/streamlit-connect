package io.streamlitconnect.server.grpc;

import io.streamlitconnect.RootContainer;

class RootContainerImpl extends ContainerImpl implements RootContainer {

    public static final String KEY = "root";

    RootContainerImpl(GrpcOperationsRequestContext context) {
        super(KEY, null, context);
    }

}

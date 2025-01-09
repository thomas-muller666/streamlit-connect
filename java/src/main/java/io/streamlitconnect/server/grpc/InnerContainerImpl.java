package io.streamlitconnect.server.grpc;

import io.streamlitconnect.InnerContainer;
import lombok.Getter;
import lombok.NonNull;

/**
 * See <a href="https://docs.streamlit.io/library/api-reference/layout/container">Container Streamlit API</a>
 */
@Getter
class InnerContainerImpl extends ContainerImpl implements InnerContainer {

    private final int height;

    private final boolean border;

    InnerContainerImpl(
        @NonNull ContainerImpl parent,
        GrpcOperationsRequestContext context,
        int height, // <= 0 means auto height
        boolean border
    ) {
        super("inner_" + Utils.randomKeySuffix(), parent, context);
        this.height = height;
        this.border = border;
    }

}

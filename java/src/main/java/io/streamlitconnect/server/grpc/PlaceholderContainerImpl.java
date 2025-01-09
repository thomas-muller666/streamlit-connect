package io.streamlitconnect.server.grpc;

import io.streamlitconnect.PlaceholderContainer;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.PlaceholderContainerOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.StreamlitOperation;
import lombok.Getter;

/**
 * See <a href="https://docs.streamlit.io/library/api-reference/layout/container">Container Streamlit API</a>
 */
@Getter
class PlaceholderContainerImpl extends ContainerImpl implements PlaceholderContainer {

    PlaceholderContainerImpl(
        ContainerImpl parent,
        GrpcOperationsRequestContext context
    ) {
        super("placeholder_" + Utils.randomKeySuffix(), parent, context);
    }


    @Override
    public void empty() {
        PlaceholderContainerOp.Builder builder = PlaceholderContainerOp.newBuilder()
            .setKey(getKey())
            .setEmpty(true);

        StreamlitOperation op = StreamlitOperation.newBuilder().setPlaceholderContainerOp(builder.build()).build();
        log.debug("Queuing PlaceholderContainer (empty) operation: {}", op);
        getContext().enqueueOp(op);
    }

}

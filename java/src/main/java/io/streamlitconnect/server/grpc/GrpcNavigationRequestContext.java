package io.streamlitconnect.server.grpc;

import io.grpc.stub.StreamObserver;
import io.streamlitconnect.NavigationMenu;
import io.streamlitconnect.NavigationMenu.MenuItem;
import io.streamlitconnect.NavigationRequestContext;
import io.streamlitconnect.StreamlitApp;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.Action;
import io.streamlitconnect.server.grpc.gen.StreamlitNavigationProto;
import io.streamlitconnect.server.grpc.gen.StreamlitNavigationProto.StreamlitNavigation;
import io.streamlitconnect.server.grpc.gen.StreamlitNavigationProto.StreamlitNavigationRequest;
import java.util.List;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GrpcNavigationRequestContext extends GrpcStreamlitRequestContext implements NavigationRequestContext {

    private static final Logger log = LoggerFactory.getLogger(GrpcNavigationRequestContext.class);

    GrpcNavigationRequestContext(
        @NonNull GrpcStreamlitSessionContext sessionContext,
        int sequenceNumber,
        StreamlitNavigationRequest request,
        StreamObserver<?> responseObserver) {
        super(sessionContext, sequenceNumber, request, responseObserver);
    }

    protected void handleRequest(@NonNull StreamlitApp app) {
        if (isCancelled() || isClosed()) {
            return; // Abort
        }

        try {
            StreamlitNavigationRequest request = (StreamlitNavigationRequest) getRequest();
            StreamObserver<StreamlitNavigation> responseObserver = (StreamObserver<StreamlitNavigation>) getResponseObserver();

            // Process the actions
            List<Action> actions = request.getActionsList();
            sessionContext.processActions(actions);

            NavigationMenu menu = app.getNavigationMenu(this);
            StreamlitNavigation.Builder navBuilder = StreamlitNavigation.newBuilder();

            if (menu != null) {
                buildNavigationResponse(navBuilder, menu);
            }

            StreamlitNavigation nav = navBuilder.build();
            waitForTasks(); // Wait for all tasks to complete before sending the response

            log.debug("Sending navigation response {}", nav);
            responseObserver.onNext(nav);

            // Signal that we're done with this request
            responseObserver.onCompleted();

            close();

            log.debug("Streamlit getNavigation completed for session: {}", getSessionContext().getSessionId());
        } finally {
            // Signal to the session context that we're done with this request
            sessionContext.signalNavigationRequestFinished();
        }
    }

    private void buildNavigationResponse(StreamlitNavigation.Builder navBuilder, NavigationMenu menu) {
        navBuilder.setLocationValue(menu.getLocation().ordinal());

        for (MenuItem item : menu.getItems()) {
            StreamlitNavigationProto.MenuItem.Builder menuItemBuilder = StreamlitNavigationProto.MenuItem.newBuilder();

            if (item.header() != null) {
                menuItemBuilder.setHeader(item.header());
            }

            for (NavigationMenu.NavigationEntry entry : item.entries()) {
                StreamlitNavigationProto.NavigationEntry.Builder entryBuilder = StreamlitNavigationProto.NavigationEntry.newBuilder()
                    .setPage(entry.pageName())
                    .setTitle(entry.title())
                    .setIsDefault(entry.isDefault());

                if (entry.icon() != null) {
                    entryBuilder.setIcon(entry.icon());
                }
                menuItemBuilder.addEntries(entryBuilder);
            }
            StreamlitNavigationProto.MenuItem menuItem = menuItemBuilder.build();
            navBuilder.addItems(menuItem);
        }
    }

}

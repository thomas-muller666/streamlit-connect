package io.streamlitconnect.server.grpc;

import io.grpc.stub.StreamObserver;
import io.streamlitconnect.OperationsRequestContext;
import io.streamlitconnect.StreamlitApp;
import io.streamlitconnect.StreamlitException;
import io.streamlitconnect.server.grpc.gen.StreamlitActionsProto.Action;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.EndOp;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.StreamlitOperation;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.StreamlitOperation.OperationCase;
import io.streamlitconnect.server.grpc.gen.StreamlitOperationsProto.StreamlitOperationsRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class GrpcOperationsRequestContext extends GrpcStreamlitRequestContext implements OperationsRequestContext {

    private final static String DEFAULT_PAGE_NAME = "default";

    private final static Logger log = LoggerFactory.getLogger(GrpcOperationsRequestContext.class);

    // Use a BlockingQueue to keep track of the Streamlit operations to send back to the client
    @Getter(AccessLevel.MODULE)
    private final BlockingQueue<StreamlitOperation> operationsQueue = new LinkedBlockingQueue<>();

    @Getter
    private final RootContainerImpl rootContainer = new RootContainerImpl(this);

    @Getter
    private final SidebarContainerImpl sidebarContainer = new SidebarContainerImpl(this);

    private final AtomicReference<Thread> processingOpsThread = new AtomicReference<>();

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String pageName;

    GrpcOperationsRequestContext(
        @NonNull GrpcStreamlitSessionContext sessionContext,
        int sequenceNumber,
        Object request,
        StreamObserver<?> responseObserver
    ) {
        super(sessionContext, sequenceNumber, request, responseObserver);
    }

    @Override
    public void cancel() {
        operationsQueue.clear();
        addEndSignal(false, true);
        super.cancel();
        Optional.ofNullable(processingOpsThread.get()).ifPresent(Thread::interrupt);
    }

    @Override
    public void close() {
        super.close();
        operationsQueue.clear();
    }

    @Override
    protected void handleRequest(@NonNull StreamlitApp app) {
        if (isCancelled() || isClosed()) {
            return; // Abort
        }

        try {
            StreamlitOperationsRequest request = (StreamlitOperationsRequest) getRequest();
            StreamObserver<StreamlitOperation> responseObserver = (StreamObserver<StreamlitOperation>) getResponseObserver();

            // Process the actions
            List<Action> actions = request.getActionsList();
            sessionContext.processActions(actions);

            // Start a new thread for processing the operations (blocking queue)
            startProcessingOperations();

            pageName = request.getPage();
            if (StringUtils.isBlank(pageName)) {
                pageName = DEFAULT_PAGE_NAME;
            }

            // Render the app
            log.debug("Rendering app: {}", app);
            app.render(this);

            // Wait for all tasks to complete before sending the END ops signal
            waitForTasks();

            // Add the END ops signal
            addEndSignal();

            // Wait for the processingOpsThread to finish
            try {
                processingOpsThread.get().join();
            } catch (InterruptedException e) {
                Thread.interrupted(); // Restore the interrupted status
                throw new StreamlitException(e);
            }

            // Close the context
            close();

            log.debug("Streamlit getOperations completed for session: {}", getSessionContext().getSessionId());
        } finally {
            // Signal to the session context that we're done with this request
            sessionContext.signalOperationsRequestFinished();
        }
    }

    void enqueueOp(@NonNull StreamlitOperation operation) {
        log.debug("Enqueueing operation: {}", operation);
        operationsQueue.add(operation);
    }

    private void startProcessingOperations() {
        processingOpsThread.set(Thread.startVirtualThread(this::processOperations));
    }

    private void processOperations() {
        Utils.prepareMDC(sessionContext.getSessionId(), sessionContext.getCurrentSequenceNumber());

        log.debug("Processing operations thread started");

        StreamObserver<StreamlitOperation> responseObserver = (StreamObserver<StreamlitOperation>) getResponseObserver();
        try {
            while (true) {
                StreamlitOperation operation = operationsQueue.take();
                log.debug("Operation popped from queue: {}", operation);
                if (operation.getOperationCase() == OperationCase.ENDOP) {
                    sessionContext.resetAllWidgetsChangedFlags(); // All touches have been handled by now, reset the touched flags
                    responseObserver.onNext(operation);
                    responseObserver.onCompleted();
                    break;
                }
                responseObserver.onNext(operation);
            }
        } catch (InterruptedException e) {
            Thread.interrupted(); // Restore the interrupted status
            throw new StreamlitException(e);
        }
        log.debug("Processing operations thread finished");
        MDC.clear();
    }

    private void addEndSignal() {
        addEndSignal(false, false);
    }

    private void addEndSignal(boolean terminate, boolean cancelled) {
        EndOp.Builder builder = EndOp.newBuilder()
            .setTerminateSession(terminate)
            .setCancelled(cancelled);

        StreamlitOperation op = StreamlitOperation.newBuilder().setEndOp(builder.build()).build();
        enqueueOp(op);
    }
}


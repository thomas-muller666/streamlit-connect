package io.streamlitconnect.server.grpc;

import static org.apache.commons.lang3.Validate.isTrue;

import io.grpc.stub.StreamObserver;
import io.streamlitconnect.StreamlitApp;
import io.streamlitconnect.StreamlitException;
import io.streamlitconnect.StreamlitRequestContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class GrpcStreamlitRequestContext implements StreamlitRequestContext {

    private final static Logger log = LoggerFactory.getLogger(GrpcStreamlitRequestContext.class);

    @Getter(AccessLevel.PACKAGE)
    private final StreamObserver<?> responseObserver;

    @Getter
    protected final GrpcStreamlitSessionContext sessionContext;

    @Getter
    private final int sequenceNumber;

    @Getter(AccessLevel.PACKAGE)
    private final Object request;

    @Getter
    private final Instant createdAt = Instant.now();

    @Getter
    private Instant closedAt;

    private final Map<String, Object> attributes = new HashMap<>();

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private final List<Future<?>> futures = new CopyOnWriteArrayList<>();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    GrpcStreamlitRequestContext(
        @NonNull GrpcStreamlitSessionContext sessionContext,
        int sequenceNumber,
        Object request,
        StreamObserver<?> responseObserver
    ) {
        isTrue(sequenceNumber >= 1, "sequenceNumber must be >= 0");
        this.sessionContext = sessionContext;
        this.sequenceNumber = sequenceNumber;
        this.request = request;
        this.responseObserver = responseObserver;
    }

    @Override
    public Object getAttribute(@NonNull String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(@NonNull String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void execute(@NonNull Runnable runnable) {
        Future<?> future = executorService.submit(runnable);
        futures.add(future);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            closedAt = Instant.now();
            log.debug("Closing request context: {}", this);
            attributes.clear();
            futures.clear();
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(sessionContext.getSessionId())
            .append(sequenceNumber)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GrpcStreamlitRequestContext other = (GrpcStreamlitRequestContext) obj;
        return sessionContext.getSessionId().equals(other.sessionContext.getSessionId()) &&
            sequenceNumber == other.sequenceNumber;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("sessionId", sessionContext.getSessionId())
            .append("appName", sessionContext.getAppName())
            .append("sequenceNumber", sequenceNumber)
            .append("request", request)
            .append("responseObserver", responseObserver)
            .append("closed", closed)
            .append("cancelled", cancelled)
            .append("createdAt", createdAt)
            .append("closedAt", closedAt)
            .append("attributes", attributes)
            .toString();
    }

    protected abstract void handleRequest(@NonNull StreamlitApp app);

    void waitForTasks() {
        log.debug("Waiting for #tasks to complete: {}", futures.size());
        futures.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                throw new StreamlitException("Error waiting for task: " + e.getMessage(), e);
            }
        });
        futures.clear();
    }

    void cancel() {
        if ((!closed.get()) && cancelled.compareAndSet(false, true)) {
            log.debug("Cancelling request context: {}", this);
//            responseObserver.onError(new StreamlitException("Request context cancelled: " + this));
            responseObserver.onCompleted();
            cancelTasks();
            close();
        }
    }

    private void cancelTasks() {
        log.debug("Cancelling #tasks: {}", futures.size());
        futures.forEach(future -> future.cancel(true));
        executorService.shutdownNow();
        futures.clear();
    }

}

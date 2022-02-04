package eu.pkgsoftware.babybuddywidgets.networking;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import androidx.annotation.NonNull;

public class ChildrenStateTracker {
    public static class CancelledException extends Exception {};

    public interface ConnectionStateListener {
        void connectionStateChanged(boolean connected, long disconnectedFor);
    }

    private abstract class DeferredRequest {
        public long startTime = System.currentTimeMillis();

        public long getScheduledTime() {
            return startTime;
        }

        protected void retry() {
            startTime = System.currentTimeMillis();
            requestQueue.add(this);
            queueNextRequest();
        }

        public abstract void cancel();
        public abstract void doRequest();
    };

    private static final int DEFER_BASE_TIMEOUT = 500;
    private static final int MAX_DEFERRED_TIMEOUT = 20000;

    private BabyBuddyClient client;
    private boolean closed = false;

    private Handler queueHandler = null;
    private CancellableRequest pendingRequest = null;

    private boolean connected = false;
    private ConnectionStateListener connectionStateListener = null;
    private long disconnectedStartTime = 0;
    private int disconnectRetryCounter = 0;

    private final ArrayList<DeferredRequest> requestQueue = new ArrayList<>();

    private static abstract class CancellableRequest implements Runnable {
        public DeferredRequest deferredRequest;

        public CancellableRequest(DeferredRequest req) {
            deferredRequest = req;
        }

        private boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        public void run() {
            if (!cancelled) {
                runIfNotCancelled();
            }
        }

        public abstract void runIfNotCancelled();
    };

    public ChildrenStateTracker(BabyBuddyClient client, Looper looper) {
        this.client = client;
        this.queueHandler = new Handler(looper);

        connected = true;
        setDisconnected();
    }

    private void queueNextRequest() {
        if (closed || (requestQueue.size() <= 0)) {
            return;
        }

        Collections.sort(requestQueue, (a, b) -> Long.compare(a.getScheduledTime(), b.getScheduledTime()));
        final DeferredRequest req = requestQueue.get(0);

        if (pendingRequest != null) {
            if (req.getScheduledTime() < pendingRequest.deferredRequest.getScheduledTime()) {
                pendingRequest.cancel();
                requestQueue.add(pendingRequest.deferredRequest);
            } else {
                return;
            }
        }
        requestQueue.remove(0);

        pendingRequest = new CancellableRequest(req) {
            @Override
            public void runIfNotCancelled() {
                if (closed) {
                    req.cancel();
                    pendingRequest = null;
                    return;
                }
                pendingRequest = null;

                System.out.println("Sending request at " + System.currentTimeMillis() +  ": " + req);
                req.doRequest();
                queueNextRequest();
            }
        };

        long exponentialBackoff = 0;
        if (disconnectRetryCounter > 0) {
            exponentialBackoff = Math.min(
                MAX_DEFERRED_TIMEOUT,
                (long) Math.pow(1.5, disconnectRetryCounter - 1)* DEFER_BASE_TIMEOUT
            );
        }

        queueHandler.postDelayed(
            pendingRequest,
            Math.max(0, req.getScheduledTime() - System.currentTimeMillis() + 50 + exponentialBackoff)
        );
    }

    private class QueueRequest<R> {
        public void queue(
                Consumer<BabyBuddyClient.RequestCallback<R>> request,
                BabyBuddyClient.RequestCallback<R> responseCallback) {
            requestQueue.add(new DeferredRequest() {
                @Override
                public void cancel() {
                    responseCallback.error(new CancelledException());
                }

                @Override
                public void doRequest() {
                    BabyBuddyClient.RequestCallback<R> local = new BabyBuddyClient.RequestCallback<R>(){
                        @Override
                        public void error(Exception error) {
                            setDisconnected();
                            retry();
                        }

                        @Override
                        public void response(R response) {
                            setConnected();
                            responseCallback.response(response);
                        }
                    };
                    request.accept(local);
                }

                @NonNull
                @Override
                public String toString() {
                    return "DeferredRequest{requesting" + this.getClass().getGenericSuperclass() + "}";
                }
            });
            queueNextRequest();
        }
    }

    private void setConnected() {
        if (!connected) {
            connected = true;
            if (connectionStateListener != null) {
                connectionStateListener.connectionStateChanged(connected, 0);
            }
        }
    }

    private void setDisconnected() {
        if (connected) {
            disconnectRetryCounter = 0;
            connected = false;
            disconnectedStartTime = System.currentTimeMillis();

            final Runnable disconnectUpdateFunction = new Runnable() {
                @Override
                public void run() {
                    if (closed || connected) {
                        return;
                    }
                    if (connectionStateListener != null) {
                        connectionStateListener.connectionStateChanged(
                            connected, System.currentTimeMillis() - disconnectedStartTime
                        );
                    }
                    queueHandler.postDelayed(this, 1000);
                }
            };
            disconnectUpdateFunction.run();
        } else {
            disconnectRetryCounter++;
            if (disconnectRetryCounter > 10000) {
                disconnectRetryCounter = 10000;
            }
        }
    }

    public boolean isConnected() {
        return connected && !closed;
    }

    public void close() {
        closed = true;
        for (DeferredRequest r : requestQueue) {
            r.cancel();
        }
        requestQueue.clear();
    }

    public void setConnectionStateListener(ConnectionStateListener l) {
        connectionStateListener = l;
    }

    public void resetDisconnectTimer() {
        disconnectedStartTime = System.currentTimeMillis();
        disconnectRetryCounter = 0;
        queueNextRequest();
    }

    private abstract class StateObserver {
        private long requestInterval;
        private boolean closed = false;
        private boolean requeued = true;

        public StateObserver(long requestInterval) {
            this.requestInterval = requestInterval;
            queueHandler.post(() -> update());
        }

        public void close() {
            closed = true;
        }

        public boolean isClosed() {
            return closed || ChildrenStateTracker.this.closed;
        }

        private void update() {
            if (isClosed() || !requeued) {
                return;
            }
            requeued = false;
            queueHandler.postDelayed(() -> update(), requestInterval);
            queueRequests();
        }

        protected void requeue() {
            requeued = true;
        }

        protected abstract void queueRequests();
    }

    /* Child lists */
    public interface ChildrenListListener {
        void childrenListUpdated(BabyBuddyClient.Child[] children);
    }

    public class ChildListObserver extends StateObserver {
        public static final long INTERVAL = 10000;

        private ChildrenListListener listener;
        private BabyBuddyClient.Child[] childrenList = null;

        public ChildListObserver(ChildrenListListener listener) {
            super(INTERVAL);

            this.listener = listener;
        }

        @Override
        protected void queueRequests() {
            new QueueRequest<BabyBuddyClient.Child[]>().queue(
                client::listChildren,
                new BabyBuddyClient.RequestCallback<BabyBuddyClient.Child[]>() {
                    @Override
                    public void error(Exception error) {
                        requeue();
                    }

                    @Override
                    public void response(BabyBuddyClient.Child[] response) {
                        requeue();
                        if ((childrenList == null) || (!Arrays.equals(response, childrenList))) {
                            childrenList = response;
                            if (listener != null) {
                                listener.childrenListUpdated(response);
                            }
                        }
                    }
                }
            );
        }
    }

    /* Child listener */
    public interface ChildListener {
        void childValidUpdated(boolean valid);
        void timersUpdated(BabyBuddyClient.Timer[] timers);
    }

    public class ChildObserver extends StateObserver {
        public static final long INTERVAL = 1000;

        private int childId;
        private ChildListener listener;
        private boolean closed = false;
        private BabyBuddyClient.Timer[] currentTimerList = null;

        public ChildObserver(int childId, ChildListener listener) {
            super(INTERVAL);

            this.childId = childId;
            this.listener = listener;
        }

        private class BoundTimerListCall {
            public void call(BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]> callback) {
                client.listTimers(childId, callback);
            }
        }

        protected void queueRequests() {
            new QueueRequest<BabyBuddyClient.Timer[]>().queue(
                new BoundTimerListCall()::call,
                new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
                    @Override
                    public void error(Exception error) {
                        requeue();
                    }

                    @Override
                    public void response(BabyBuddyClient.Timer[] response) {
                        requeue();

                        if (isClosed()) {
                            return;
                        }

                        if ((currentTimerList == null) || (!Arrays.equals(currentTimerList, response))) {
                            currentTimerList = response;
                            listener.timersUpdated(response);
                        }
                    }
                }
            );
        }
    }
}

package eu.pkgsoftware.babybuddywidgets.networking;

import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import androidx.annotation.NonNull;

public class ChildrenStateTracker {
    public static class CancelledException extends Exception {
    }

    ;

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
    }

    ;

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
    }

    ;

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

                req.doRequest();
                queueNextRequest();
            }
        };

        long exponentialBackoff = 0;
        if (disconnectRetryCounter > 0) {
            exponentialBackoff = Math.min(
                MAX_DEFERRED_TIMEOUT,
                (long) Math.pow(1.5, disconnectRetryCounter - 1) * DEFER_BASE_TIMEOUT
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
                    BabyBuddyClient.RequestCallback<R> local = new BabyBuddyClient.RequestCallback<R>() {
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
            queueHandler.post(this::update);
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
            queueHandler.postDelayed(this::update, requestInterval);
            queueRequests();
        }

        protected void forceUpdate() {
            if (isClosed()) return;
            requeued = true;
            queueHandler.postDelayed(this::update, 0);
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

        private final int childId;
        private final ChildListener listener;
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

    /* Timeline listener */
    public interface TimelineListener {
        void sleepRecordsObtained(int offset, BabyBuddyClient.TimeEntry[] entries);

        void tummyTimeRecordsObtained(int offset, BabyBuddyClient.TimeEntry[] entries);

        void feedingRecordsObtained(int offset, BabyBuddyClient.TimeEntry[] entries);

        void changeRecordsObtained(int offset, BabyBuddyClient.TimeEntry[] entries);
    }

    public class TimelineObserver extends StateObserver {
        public static final long INTERVAL = 5000;
        public static final int COUNT = 20;

        private final int childId;
        private final TimelineListener listener;
        private boolean closed = false;
        private int requeueGate = 0;

        public Map<String, Integer> queryOffsets = new HashMap<>();

        public TimelineObserver(int childId, TimelineListener listener) {
            super(INTERVAL);
            this.childId = childId;
            this.listener = listener;
        }

        public int offsetByName(String name) {
            Integer result = queryOffsets.getOrDefault(name, null);
            if (result == null) {
                return 0;
            }
            return result;
        }

        private class WithOffset {
            protected final int offset;

            WithOffset(int offset) {
                this.offset = offset;
            }
        }

        private class BoundSleepRecordsCallback extends WithOffset {
            BoundSleepRecordsCallback(int offset) {
                super(offset);
            }

            public void call(BabyBuddyClient.RequestCallback<BabyBuddyClient.TimeEntry[]> callback) {
                client.listSleepEntries(
                    childId,
                    offset,
                    COUNT,
                    callback
                );
            }
        }

        private class BoundFeedingRecordsCallback extends WithOffset {
            BoundFeedingRecordsCallback(int offset) {
                super(offset);
            }

            public void call(BabyBuddyClient.RequestCallback<BabyBuddyClient.FeedingEntry[]> callback) {
                client.listFeedingsEntries(
                    childId,
                    offset,
                    COUNT,
                    callback
                );
            }
        }

        private class BoundTummyTimeRecordsCallback extends WithOffset {
            BoundTummyTimeRecordsCallback(int offset) {
                super(offset);
            }

            public void call(BabyBuddyClient.RequestCallback<BabyBuddyClient.TimeEntry[]> callback) {
                client.listTummyTimeEntries(
                    childId,
                    offset,
                    COUNT,
                    callback
                );
            }
        }

        private class BoundChangeRecordsCallback extends WithOffset {
            BoundChangeRecordsCallback(int offset) {
                super(offset);
            }

            public void call(BabyBuddyClient.RequestCallback<BabyBuddyClient.ChangeEntry[]> callback) {
                client.listChangeEntries(
                    childId,
                    offset,
                    COUNT,
                    callback
                );
            }
        }

        public void forceUpdate() {
            super.forceUpdate();
        }

        @Override
        protected void requeue() {
            requeueGate--;
            if (requeueGate <= 0) {
                super.requeue();
            }
        }

        protected void queueRequests() {
            requeueGate = 4;
            final Map<String, Integer> queuedOffsets = new ArrayMap<>();
            final String[] CLASS_NAMES = {
                BabyBuddyClient.ACTIVITIES.FEEDING,
                BabyBuddyClient.ACTIVITIES.SLEEP,
                BabyBuddyClient.ACTIVITIES.TUMMY_TIME,
                BabyBuddyClient.EVENTS.CHANGE
            };
            for (String name : CLASS_NAMES) {
                queuedOffsets.put(name, offsetByName(name));
            }

            new QueueRequest<BabyBuddyClient.TimeEntry[]>().queue(
                new BoundSleepRecordsCallback(queuedOffsets.get(BabyBuddyClient.ACTIVITIES.SLEEP))::call,
                new BabyBuddyClient.RequestCallback<BabyBuddyClient.TimeEntry[]>() {
                    @Override
                    public void error(Exception error) {
                        requeue();
                    }

                    @Override
                    public void response(BabyBuddyClient.TimeEntry[] response) {
                        requeue();
                        if (isClosed()) {
                            return;
                        }
                        listener.sleepRecordsObtained(
                            queuedOffsets.get(BabyBuddyClient.ACTIVITIES.SLEEP), response
                        );
                    }
                }
            );
            new QueueRequest<BabyBuddyClient.FeedingEntry[]>().queue(
                new BoundFeedingRecordsCallback(queuedOffsets.get(BabyBuddyClient.ACTIVITIES.FEEDING))::call,
                new BabyBuddyClient.RequestCallback<BabyBuddyClient.FeedingEntry[]>() {
                    @Override
                    public void error(Exception error) {
                        requeue();
                    }

                    @Override
                    public void response(BabyBuddyClient.FeedingEntry[] response) {
                        requeue();
                        if (isClosed()) {
                            return;
                        }
                        listener.feedingRecordsObtained(
                            queuedOffsets.get(BabyBuddyClient.ACTIVITIES.FEEDING), response
                        );
                    }
                }
            );
            new QueueRequest<BabyBuddyClient.TimeEntry[]>().queue(
                new BoundTummyTimeRecordsCallback(queuedOffsets.get(BabyBuddyClient.ACTIVITIES.TUMMY_TIME))::call,
                new BabyBuddyClient.RequestCallback<BabyBuddyClient.TimeEntry[]>() {
                    @Override
                    public void error(Exception error) {
                        requeue();
                    }

                    @Override
                    public void response(BabyBuddyClient.TimeEntry[] response) {
                        requeue();
                        if (isClosed()) {
                            return;
                        }
                        listener.tummyTimeRecordsObtained(
                            queuedOffsets.get(BabyBuddyClient.ACTIVITIES.TUMMY_TIME), response
                        );
                    }
                }
            );
            new QueueRequest<BabyBuddyClient.ChangeEntry[]>().queue(
                new BoundChangeRecordsCallback(queuedOffsets.get(BabyBuddyClient.EVENTS.CHANGE))::call,
                new BabyBuddyClient.RequestCallback<BabyBuddyClient.ChangeEntry[]>() {
                    @Override
                    public void error(Exception error) {
                        requeue();
                    }

                    @Override
                    public void response(BabyBuddyClient.ChangeEntry[] response) {
                        requeue();
                        if (isClosed()) {
                            return;
                        }
                        listener.changeRecordsObtained(
                            queuedOffsets.get(BabyBuddyClient.EVENTS.CHANGE), response
                        );
                    }
                }
            );
        }
    }

}

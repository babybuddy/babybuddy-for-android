package eu.pkgsoftware.babybuddywidgets.networking;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Queue;
import java.util.function.Consumer;

public class ChildrenStateTracker {
    public static class CancelledException extends Exception {};

    public interface ConnectionStateListener {
        void connectionStateChanged(boolean connected, long disconnectedFor);
    }

    public interface ChildrenListListener {
        void childrenListUpdated(BabyBuddyClient.Child[] children);
    }

    public interface ChildListener {
        void childValidUpdated(boolean valid);
        void timersUpdated(BabyBuddyClient.Timer[] timers);
    }

    private static final int DEFER_BASE_TIMEOUT = 1000;
    private static final int MAX_DEFERRED_TIMEOUT = 20000;
    private abstract class DeferredRequest {
        public int deferredCount = 0;
        public long startTime = System.currentTimeMillis();

        public int getTimeout() {
            int timeout = 0;
            if (deferredCount > 0) {
                timeout = (int) (DEFER_BASE_TIMEOUT * Math.pow(2, deferredCount - 1));
            }
            return timeout;
        }

        public long getScheduledTime() {
            return startTime + getTimeout();
        }

        protected void retry() {
            deferredCount++;
            startTime = System.currentTimeMillis();
            requestQueue.add(0, this);
            queueNextRequest();
        }

        public abstract void cancel();
        public abstract void doRequest();
    };

    private BabyBuddyClient client;
    private Integer selectedChildId = null;
    private boolean closed = false;
    private boolean connected;

    private Handler queueHandler = null;

    private ConnectionStateListener connectionStateListener = null;
    private ChildrenListListener childrenListListener = null;
    private ChildListener childListener = null;

    private long disconnectedStartTime = 0;
    private long childrenListUpdateDelay = 10000;
    private long childListsUpdateDelay = 1000;

    private ArrayList<DeferredRequest> requestQueue = new ArrayList<>();

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

    private CancellableRequest pendingRequest = null;

    public ChildrenStateTracker(BabyBuddyClient client, Looper looper) {
        this.client = client;
        this.queueHandler = new Handler(looper);

        connected = true;
        setDisconnected();

        this.queueHandler.post(() -> updateChildLists());
        this.queueHandler.post(() -> updateChildrenList());
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
        queueHandler.postDelayed(
            pendingRequest,
            Math.max(0, req.getScheduledTime() - System.currentTimeMillis() + 100)
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
            });
        }
    }

    private BabyBuddyClient.Child[] childrenList = null;
    private void updateChildrenList() {
        new QueueRequest<BabyBuddyClient.Child[]>().queue(
            client::listChildren,
            new BabyBuddyClient.RequestCallback<BabyBuddyClient.Child[]>() {
                private void requeue() {
                    queueHandler.postDelayed(() -> updateChildrenList(), childrenListUpdateDelay);
                }

                @Override
                public void error(Exception error) {
                    requeue();
                }

                @Override
                public void response(BabyBuddyClient.Child[] response) {
                    requeue();
                    if ((childrenList == null) || (!Arrays.equals(response, childrenList))) {
                        childrenList = response;
                        if (childrenListListener != null) {
                            childrenListListener.childrenListUpdated(response);
                        }
                    }
                }
            }
        );
        queueNextRequest();
    }

    private class BoundTimerListCall {
        private int childId;

        public BoundTimerListCall(int childId) {
            this.childId = childId;
        }

        public void call(BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]> callback) {
            client.listTimers(childId, callback);
        }
    }

    private BabyBuddyClient.Timer[] selectedChildTimerList = null;
    private void updateChildLists() {
        if (selectedChildId == null) {
            return;
        }

        final int requestedForChildId = selectedChildId;
        System.out.println("Update child lists for " + selectedChildId);

        new QueueRequest<BabyBuddyClient.Timer[]>().queue(
            new BoundTimerListCall(selectedChildId)::call,
            new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
                private void requeue() {
                    if (requestedForChildId != selectedChildId) {
                        return;
                    }
                    queueHandler.postDelayed(() -> updateChildLists(), childListsUpdateDelay);
                }

                @Override
                public void error(Exception error) {
                    requeue();
                }

                @Override
                public void response(BabyBuddyClient.Timer[] response) {
                    requeue();

                    if (requestedForChildId != selectedChildId) {
                        return;
                    }

                    System.out.println("AAA " + selectedChildTimerList + "  " +  response);
                    if ((selectedChildTimerList == null) || (!Arrays.equals(selectedChildTimerList, response))) {
                        selectedChildTimerList = response;

                        if (childListener != null) {
                            childListener.timersUpdated(response);
                        }
                    }
                }
            }
        );
        queueNextRequest();
    }

    public void selectChild(Integer childId) {
        if (selectedChildId != childId) {
            selectedChildId = childId;
            selectedChildTimerList = null;
            updateChildLists();
        }
    }

    public Integer getSelectedChildId() {
        return selectedChildId;
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

    public void setChildrenListListener(ChildrenListListener l) {
        childrenListListener = l;
        if (childrenList != null) {
            queueHandler.post(() -> childrenListListener.childrenListUpdated(childrenList));
        }
    }

    public void setChildListener(ChildListener l) {
        childListener = l;
    }

    public void setConnectionStateListener(ConnectionStateListener l) {
        connectionStateListener = l;
    }

    public void resetDisconnectTimer() {
        disconnectedStartTime = System.currentTimeMillis();
    }
}

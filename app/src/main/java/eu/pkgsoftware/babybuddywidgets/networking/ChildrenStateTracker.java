package eu.pkgsoftware.babybuddywidgets.networking;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
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

        protected void retry() {
            deferredCount++;
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
    private ChildListener childrenListener = null;

    private long disconnectedStartTime = 0;
    private long childrenListUpdateDelay = 10000;
    private long childListsUpdateDelay = 1000;

    private ArrayList<DeferredRequest> requestQueue = new ArrayList<>();
    private boolean requestPending = false;

    public ChildrenStateTracker(BabyBuddyClient client, Looper looper) {
        this.client = client;
        this.queueHandler = new Handler(looper);

        connected = true;
        setDisconnected();

        this.queueHandler.post(() -> updateChildLists());
        this.queueHandler.post(() -> updateChildrenList());
    }

    private void queueNextRequest() {
        if (closed || requestPending || (requestQueue.size() <= 0)) {
            return;
        }
        final DeferredRequest req = requestQueue.remove(0);

        int timeout = 0;
        if (req.deferredCount > 0) {
            timeout = (int) (DEFER_BASE_TIMEOUT * Math.pow(2, req.deferredCount - 1));
        }

        requestPending = true;
        queueHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (closed) {
                    req.cancel();
                    requestPending = false;
                    return;
                }
                requestPending = false;
                req.doRequest();
                queueNextRequest();
            }
        }, timeout);
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
    private boolean compareChildrenLists(BabyBuddyClient.Child[] a, BabyBuddyClient.Child[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }

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
                    if ((childrenList == null) || (!compareChildrenLists(response, childrenList))) {
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

    private void updateChildLists() {
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
        return connected;
    }

    public void close() {
        closed = true;
        for (DeferredRequest r : requestQueue) {
            r.cancel();
        }
        requestQueue.clear();
    }

    public  void setChildrenListListener(ChildrenListListener l) {
        childrenListListener = l;
        if (childrenList != null) {
            queueHandler.post(() -> childrenListListener.childrenListUpdated(childrenList));
        }
    }

    public void setConnectionStateListener(ConnectionStateListener l) {
        connectionStateListener = l;
    }

    public void resetDisconnectTimer() {
        disconnectedStartTime = System.currentTimeMillis();
    }
}

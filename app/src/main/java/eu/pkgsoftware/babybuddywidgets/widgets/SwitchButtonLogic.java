package eu.pkgsoftware.babybuddywidgets.widgets;

import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;

public class SwitchButtonLogic {
    public interface StateCallback {
        void call(boolean state, boolean userClick);
    }

    private ImageButton onButton, offButton;
    private boolean state;
    private ArrayList<StateCallback> callbacks = new ArrayList<>();

    public SwitchButtonLogic(ImageButton onButton, ImageButton offButton, boolean state) {
        this.onButton = onButton;
        this.offButton = offButton;
        this.state = state;

        onButton.setOnClickListener(v -> setState(true, true));
        offButton.setOnClickListener(v -> setState(false, true));

        updateVisibilityState();
    }

    private void updateVisibilityState() {
        onButton.setVisibility(state ? View.GONE : View.VISIBLE);
        offButton.setVisibility(state ? View.VISIBLE : View.GONE);
    }

    private void setState(boolean b, boolean userInduced) {
        state = b;
        updateVisibilityState();

        for (StateCallback c : callbacks) {
            c.call(state, userInduced);
        }
    }

    public void setState(boolean b) {
        setState(b, false);
    }

    public boolean getState() {
        return state;
    }

    public void addStateListener(StateCallback c) {
        callbacks.add(c);
    }

    public void removeStateListener(StateCallback c) {
        callbacks.remove(c);
    }
}

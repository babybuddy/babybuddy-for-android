package eu.pkgsoftware.babybuddywidgets;

import android.animation.LayoutTransition;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;

public class NotesEditorLogic {
    private MainActivity activity;
    private NotesEditorBinding binding;
    private boolean visible;
    private String id = null;
    private CredStore credStore;

    private void updateVisibility() {
        ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
        params.height = visible ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        binding.getRoot().setLayoutParams(params);
    }

    public NotesEditorLogic(MainActivity activity, NotesEditorBinding binding, boolean visible) {
        this.activity = activity;
        this.visible = visible;
        this.binding = binding;
        credStore = activity.getCredStore();

        updateVisibility();

        LayoutTransition lt = new LayoutTransition();
        lt.setDuration(200);
        lt.enableTransitionType(LayoutTransition.CHANGING);
        lt.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        lt.enableTransitionType(LayoutTransition.DISAPPEARING);
        binding.getRoot().setLayoutTransition(lt);

        binding.noteEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                credStore.setObjectNotes(id, binding.noteEditor.getText().toString());
            }
        });
        binding.noteEditor.setOnFocusChangeListener((v, hasFocus) -> credStore.storePrefs());
    }

    public void setVisible(boolean b) {
        if (b && (id != null)) {
            String notes = activity.getCredStore().getObjectNotes(id);
            binding.noteEditor.setText(notes == null ? "" : notes);
        }

        visible = b;
        updateVisibility();
    }

    public void setIdentifier(String id) {
        this.id = id;
        setVisible(visible);
    }

    public String getText() {
        return "" + binding.noteEditor.getText();
    }

    public void clearText() {
        if (id != null) {
            credStore.setObjectNotes(id, null);
            credStore.storePrefs();
        }
        binding.noteEditor.setText("");
    }
}

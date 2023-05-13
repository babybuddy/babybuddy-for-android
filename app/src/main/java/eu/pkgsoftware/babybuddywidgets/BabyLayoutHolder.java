package eu.pkgsoftware.babybuddywidgets;

import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.history.ChildEventHistoryLoader;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;
import eu.pkgsoftware.babybuddywidgets.timers.TimerListProvider;
import eu.pkgsoftware.babybuddywidgets.widgets.SwitchButtonLogic;

public class BabyLayoutHolder extends RecyclerView.ViewHolder {
    private final BabyManagerBinding binding;
    private final BaseFragment baseFragment;
    private final BabyBuddyClient client;
    private final CredStore credStore;
    private TimerListProvider timerListProvider = null;

    private BabyBuddyClient.Child child = null;

    private boolean changeWet = false;
    private boolean changeSolid = false;

    private NotesEditorLogic notesEditor;
    private SwitchButtonLogic notesSwitch;
    private ChildEventHistoryLoader childHistoryLoader = null;

    private ChildrenStateTracker.ChildObserver childObserver = null;

    public BabyLayoutHolder(BaseFragment fragment, BabyManagerBinding bmb) {
        super(bmb.getRoot());
        binding = bmb;

        baseFragment = fragment;
        credStore = fragment.getMainActivity().getCredStore();
        client = fragment.getMainActivity().getClient();

        GridLayoutManager l = new GridLayoutManager(binding.timersList.getContext(), 1);
        binding.timersList.setLayoutManager(l);

        View.OnClickListener invertSolid = view -> {
            changeSolid = !changeSolid;
            updateDiaperBar();
        };
        binding.solidEnabledButton.setOnClickListener(invertSolid);
        binding.solidDisabledButton.setOnClickListener(invertSolid);

        View.OnClickListener invertWet = view -> {
            changeWet = !changeWet;
            updateDiaperBar();
        };
        binding.wetEnabledButton.setOnClickListener(invertWet);
        binding.wetDisabledButton.setOnClickListener(invertWet);
        binding.sendChangeButton.setOnClickListener(view -> storeDiaperChange());

        notesSwitch = new SwitchButtonLogic(
            binding.addNoteButton,
            binding.removeNoteButton,
            false
        );

        NotesEditorBinding notesEditorBinding = NotesEditorBinding.inflate(
            fragment.getMainActivity().getLayoutInflater()
        );
        binding.diaperNotesSlot.addView(notesEditorBinding.getRoot());

        notesEditor = new NotesEditorLogic(
            fragment.getMainActivity(),
            notesEditorBinding,
            false
        );
        notesSwitch.addStateListener((b, userInduced) -> notesEditor.setVisible(b));

        binding.mainScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (childHistoryLoader != null) {
                childHistoryLoader.updateTop();
            }
        });
    }

    public BabyBuddyClient.Child getChild() {
        return child;
    }

    private void resetDiaperUi() {
        changeSolid = false;
        changeWet = false;
        updateDiaperBar();
    }

    private void updateDiaperBar() {
        binding.sendChangeButton.setVisibility((changeSolid || changeWet) ? View.VISIBLE : View.INVISIBLE);
        binding.solidEnabledButton.setVisibility(changeSolid ? View.VISIBLE : View.GONE);
        binding.solidDisabledButton.setVisibility(!changeSolid ? View.VISIBLE : View.GONE);
        binding.wetEnabledButton.setVisibility(changeWet ? View.VISIBLE : View.GONE);
        binding.wetDisabledButton.setVisibility(!changeWet ? View.VISIBLE : View.GONE);
    }

    private void storeDiaperChange() {
        client.createChangeRecord(child, changeWet, changeSolid, notesEditor.getText(),
            new BabyBuddyClient.RequestCallback<Boolean>() {
                @Override
                public void error(Exception error) {
                    baseFragment.showError(
                        true,
                        "Failed to save",
                        "Diaper change not saved"
                    );
                }

                @Override
                public void response(Boolean response) {
                    notesEditor.clearText();
                    notesSwitch.setState(false);
                    childHistoryLoader.forceRefresh();
                }
            }
        );

        resetDiaperUi();
    }

    private void requeueImmediateTimerListRefresh() {
        client.listTimers(child.id, new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
            @Override
            public void error(Exception error) {
            }

            @Override
            public void response(BabyBuddyClient.Timer[] response) {
                updateTimerList(response);
            }
        });
    }

    private void resetChildHistoryLoader() {
        if (childHistoryLoader != null) {
            childHistoryLoader.close();
        }
        childHistoryLoader = null;
        binding.timersList.setAdapter(null);
    }

    public void updateChild(BabyBuddyClient.Child c, ChildrenStateTracker stateTracker) {
        clear();
        this.child = c;
        notesEditor.setIdentifier("diaper_" + c.slug);
        notesSwitch.setState(notesEditor.isVisible());

        if (child != null) {
            childObserver = stateTracker.new ChildObserver(child.id, this::updateTimerList);

            childHistoryLoader = new ChildEventHistoryLoader(
                baseFragment,
                binding.innerTimeline,
                child.id,
                new VisibilityCheck(binding.mainScrollView),
                binding.timelineProgressSpinner
            );
            childHistoryLoader.createTimelineObserver(stateTracker);
            timerListProvider = new TimerListProvider(baseFragment, childHistoryLoader);
            binding.timersList.setAdapter(timerListProvider);
        }
    }

    public void updateTimerList(BabyBuddyClient.Timer[] timers) {
        for (BabyBuddyClient.Timer t : timers) {
            if ((child == null) || (t.child_id != child.id)) {
                timerListProvider.updateTimers(new BabyBuddyClient.Timer[0]);
                return;
            }
        }

        timerListProvider.updateTimers(timers);
    }

    public void onViewDeselected() {
        resetChildObserver();
        resetChildHistoryLoader();
        resetDiaperUi();
    }

    private void resetChildObserver() {
        if (childObserver != null) {
            childObserver.close();
            childObserver = null;
        }
    }

    public void clear() {
        resetChildObserver();
        resetChildHistoryLoader();
        resetDiaperUi();
        child = null;
    }

    public void close() {
        clear();
        timerListProvider.close();
    }
}


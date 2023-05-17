package eu.pkgsoftware.babybuddywidgets;

import android.view.View;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.compat.BabyBuddyV2TimerAdapter;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.history.ChildEventHistoryLoader;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;
import eu.pkgsoftware.babybuddywidgets.timers.StoreActivityRouter;
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface;
import eu.pkgsoftware.babybuddywidgets.timers.TimerListProvider;
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback;
import eu.pkgsoftware.babybuddywidgets.utils.Promise;
import eu.pkgsoftware.babybuddywidgets.widgets.SwitchButtonLogic;

public class BabyLayoutHolder extends RecyclerView.ViewHolder implements TimerControlInterface {
    private final BabyManagerBinding binding;
    private final BaseFragment baseFragment;
    private final BabyBuddyClient client;
    private TimerListProvider timerListProvider = null;

    private BabyBuddyClient.Child child = null;

    private boolean changeWet = false;
    private boolean changeSolid = false;

    private NotesEditorLogic notesEditor;
    private SwitchButtonLogic notesSwitch;
    private ChildEventHistoryLoader childHistoryLoader = null;

    private ChildrenStateTracker.ChildObserver childObserver = null;
    private StoreActivityRouter storeActivityRouter;

    private BabyBuddyClient.Timer[] cachedTimers = null;
    private TimersUpdatedCallback updateTimersCallback = null;

    public BabyLayoutHolder(BaseFragment fragment, BabyManagerBinding bmb) {
        super(bmb.getRoot());
        binding = bmb;

        baseFragment = fragment;
        client = fragment.getMainActivity().getClient();

        storeActivityRouter = new StoreActivityRouter(baseFragment.getMainActivity());

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
                public void error(@NonNull Exception error) {
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
                    if (childHistoryLoader != null) {
                        childHistoryLoader.forceRefresh();
                    }
                }
            }
        );

        resetDiaperUi();
    }

    private void requeueImmediateTimerListRefresh() {
        client.listTimers(child.id, new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
            @Override
            public void error(@NonNull Exception error) {
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
            timerListProvider = new TimerListProvider(
                baseFragment,
                childHistoryLoader,
                new BabyBuddyV2TimerAdapter(child, this)
            );
            binding.timersList.setAdapter(timerListProvider);
        }
    }

    public void updateTimerList(BabyBuddyClient.Timer[] timers) {
        if (child == null) {
            cachedTimers = new BabyBuddyClient.Timer[0];
            callTimerUpdateCallback();
            return;
        }

        for (BabyBuddyClient.Timer t : timers) {
            if (t.child_id != child.id) {
                return;
            }
        }

        cachedTimers = timers;
        callTimerUpdateCallback();
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
        cachedTimers = null;
    }

    public void close() {
        clear();
        timerListProvider.close();
    }

    @Override
    public void startTimer(@NotNull BabyBuddyClient.Timer timer, @NonNull Promise<BabyBuddyClient.Timer, String> p) {
        client.restartTimer(timer.id, new BabyBuddyClient.RequestCallback<>() {
            @Override
            public void error(@NonNull Exception error) {
                p.failed(baseFragment.getString(R.string.activity_store_failure_start_timer_failed));
            }

            @Override
            public void response(BabyBuddyClient.Timer timer) {
                p.succeeded(timer);
            }
        });
    }

    @Override
    public void stopTimer(@NotNull BabyBuddyClient.Timer timer, @NonNull Promise<Object, String> p) {
        client.deleteTimer(timer.id, new BabyBuddyClient.RequestCallback<>() {
            @Override
            public void error(@NonNull Exception error) {
                p.failed(baseFragment.getString(R.string.activity_store_failure_failed_to_stop_message));
            }

            @Override
            public void response(Boolean response) {
                p.succeeded(new Object());
            }
        });
    }

    @Override
    public void storeActivity(
        @NotNull BabyBuddyClient.Timer timer,
        @NonNull String activity,
        @NonNull String notes,
        @NonNull Promise<Boolean, Exception> promise
    ) {
        storeActivityRouter.store(activity, notes, timer, new Promise<>() {
            @Override
            public void succeeded(Boolean aBoolean) {
                promise.succeeded(aBoolean);
                if (childHistoryLoader != null) {
                    childHistoryLoader.forceRefresh();
                }
            }

            @Override
            public void failed(Exception e) {
                promise.failed(e);
            }
        });
    }

    @Override
    public void registerTimersUpdatedCallback(@NonNull TimersUpdatedCallback callback) {
        updateTimersCallback = callback;
        callTimerUpdateCallback();
    }

    private void callTimerUpdateCallback() {
        if ((cachedTimers != null) && (updateTimersCallback != null)) {
            updateTimersCallback.newTimerListLoaded(cachedTimers);
        }
    }
}


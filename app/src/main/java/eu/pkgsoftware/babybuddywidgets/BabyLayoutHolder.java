package eu.pkgsoftware.babybuddywidgets;

import android.view.View;

import com.squareup.phrase.Phrase;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.activitycomponents.TimerControl;
import eu.pkgsoftware.babybuddywidgets.babymanager.InsertRemoveControlsFunction;
import eu.pkgsoftware.babybuddywidgets.babymanager.LoggingButtonController;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.history.ChildEventHistoryLoader;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;
import eu.pkgsoftware.babybuddywidgets.timers.EmptyTimerListProvider;
import eu.pkgsoftware.babybuddywidgets.timers.StoreActivityRouter;
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface;
import eu.pkgsoftware.babybuddywidgets.timers.TimerListProvider;
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback;
import eu.pkgsoftware.babybuddywidgets.timers.TranslatedException;
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

    private int pendingTimerModificationCalls = 0;

    private LoggingButtonController loggingButtonController = null;

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

        notesEditor = new NotesEditorLogic(notesEditorBinding, false);
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
        binding.timersList.setAdapter(new EmptyTimerListProvider());
    }

    public void updateChild(BabyBuddyClient.Child c, ChildrenStateTracker stateTracker) {
        clear();
        this.child = c;
        notesEditor.setNotes(new CredStoreNotes(
            "diaper_" + c.slug, baseFragment.getMainActivity().getCredStore()
        ));
        notesSwitch.setState(notesEditor.isVisible());

        if (child != null) {
            if (stateTracker == null) {
                throw new RuntimeException("StateTracker was null somehow");
            }
            childObserver = stateTracker.new ChildObserver(child.id, this::updateTimerList);

            childHistoryLoader = new ChildEventHistoryLoader(
                baseFragment,
                binding.innerTimeline,
                child.id,
                new VisibilityCheck(binding.mainScrollView),
                binding.timelineProgressSpinner,
                (entryType, exception) -> {
                    String tActivity = baseFragment.translateActivityName(entryType);

                    String msg = Phrase.from(baseFragment.getResources(), R.string.history_loading_timeline_entry_failed)
                        .put("activity", tActivity)
                        .format().toString();

                    baseFragment.getMainActivity().binding.globalErrorBubble.flashMessage(msg);
                }

            );
            timerListProvider = new TimerListProvider(baseFragment, this);
            binding.timersList.setAdapter(timerListProvider);

            loggingButtonController = new LoggingButtonController(
                baseFragment,
                binding,
                new InsertRemoveControlsFunction() {
                    @Override
                    public void insertControls(@NonNull View view) {
                        binding.loggingEditors.addView(view);
                    }

                    @Override
                    public void removeControls(@NonNull View view) {
                        binding.loggingEditors.removeView(view);
                    }
                },
                child
            );
        }
    }

    public void updateTimerList(BabyBuddyClient.Timer[] timers) {
        if (pendingTimerModificationCalls > 0) {
            // Buffer timer-list updates while a timer-modifying operation is running
            // (prevents confusing UI updates)
            return;
        }

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
        if (loggingButtonController != null) {
            loggingButtonController.storeStateForSuspend();
        }
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
        if (loggingButtonController != null) {
            loggingButtonController.storeStateForSuspend();
            loggingButtonController.destroy();
            loggingButtonController = null;
        }
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

    private class UpdateBufferingPromise<A, B> implements Promise<A, B> {
        private Promise<A, B> promise;

        public UpdateBufferingPromise(Promise<A, B> promise) {
            this.promise = promise;
            pendingTimerModificationCalls++;
        }

        @Override
        public void succeeded(A a) {
            pendingTimerModificationCalls--;
            promise.succeeded(a);
        }

        @Override
        public void failed(B b) {
            pendingTimerModificationCalls--;
            promise.failed(b);
        }
    }

    @Override
    public void createNewTimer(@NonNull BabyBuddyClient.Timer timer, @NonNull Promise<BabyBuddyClient.Timer, TranslatedException> cb) {
        baseFragment.getMainActivity().getChildTimerControl(child).createNewTimer(
            timer, new UpdateBufferingPromise<>(cb)
        );
    }

    @Override
    public void startTimer(@NotNull BabyBuddyClient.Timer timer, @NonNull Promise<BabyBuddyClient.Timer, TranslatedException> cb) {
        baseFragment.getMainActivity().getChildTimerControl(child).startTimer(
            timer, new UpdateBufferingPromise<>(cb)
        );
    }

    @Override
    public void stopTimer(@NotNull BabyBuddyClient.Timer timer, @NonNull Promise<Object, TranslatedException> cb) {
        baseFragment.getMainActivity().getChildTimerControl(child).stopTimer(
            timer, new UpdateBufferingPromise<>(cb)
        );
    }

    @Override
    public void storeActivity(
        @NotNull BabyBuddyClient.Timer timer,
        @NonNull String activity,
        @NonNull String notes,
        @NonNull Promise<Boolean, Exception> cb
    ) {
        baseFragment.getMainActivity().getChildTimerControl(child).storeActivity(
            timer,
            activity,
            notes,
            new UpdateBufferingPromise<>(cb) {
                @Override
                public void succeeded(Boolean aBoolean) {
                    super.succeeded(aBoolean);
                    if (childHistoryLoader != null) {
                        childHistoryLoader.forceRefresh();
                    }
                }
            }
        );
    }

    @Override
    public void registerTimersUpdatedCallback(@NonNull TimersUpdatedCallback callback) {
        baseFragment.getMainActivity().getChildTimerControl(child).registerTimersUpdatedCallback(callback);
        callTimerUpdateCallback();
    }

    private void callTimerUpdateCallback() {
        // urgh...
        TimerControl wrapped = (TimerControl) baseFragment.getMainActivity().getChildTimerControl(child).getWrap();
        if (cachedTimers != null) {
            wrapped.callTimerUpdateCallback(cachedTimers);
        }
    }

    @NonNull
    @Override
    public CredStore.Notes getNotes(@NonNull BabyBuddyClient.Timer timer) {
        return baseFragment.getMainActivity().getChildTimerControl(child).getNotes(timer);
    }

    @Override
    public void setNotes(@NonNull BabyBuddyClient.Timer timer, CredStore.Notes notes) {
        baseFragment.getMainActivity().getChildTimerControl(child).setNotes(timer, notes);
    }
}


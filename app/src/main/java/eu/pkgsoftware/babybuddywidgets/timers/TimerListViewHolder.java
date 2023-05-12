package eu.pkgsoftware.babybuddywidgets.timers;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;

import com.squareup.phrase.Phrase;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.BaseFragment;
import eu.pkgsoftware.babybuddywidgets.CredStore;
import eu.pkgsoftware.babybuddywidgets.MainActivity;
import eu.pkgsoftware.babybuddywidgets.NotesEditorLogic;
import eu.pkgsoftware.babybuddywidgets.R;
import eu.pkgsoftware.babybuddywidgets.StoreFunction;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure;
import eu.pkgsoftware.babybuddywidgets.widgets.SwitchButtonLogic;

public class TimerListViewHolder extends RecyclerView.ViewHolder {
    public static interface TimerListViewHolderCallback {
        void updateActivities();
    }

    private final @NotNull TimerListViewHolderCallback callbacks;
    private final @NotNull QuickTimerEntryBinding binding;

    private final @NotNull BaseFragment baseFragment;
    private final CredStore credStore;
    private final BabyBuddyClient client;
    private final Handler timerHandler;

    private final SwitchButtonLogic notesEditorSwitch;
    private final NotesEditorLogic notesEditor;

    private SwitchButtonLogic startStopLogic = null;

    private BabyBuddyClient.Timer timer = null;
    private Long timerStartTime = null;

    private boolean isClosed = false;

    private String padToLen(String s, char c, int length) {
        StringBuilder sBuilder = new StringBuilder(s);
        while (sBuilder.length() < length) {
            sBuilder.insert(0, c);
        }
        return sBuilder.toString();
    }

    private boolean newUpdatedPosted = false;

    private void updateTimerTime() {
        if (timerStartTime == null) {
            binding.currentTimerTime.setText("");
        } else {
            long diff = System.currentTimeMillis() + timerStartTime;

            int seconds = (int) diff / 1000;
            int minutes = seconds / 60;
            int hours = minutes / 60;

            binding.currentTimerTime.setText(
                "HH:MM:ss"
                    .replaceAll("HH", "" + hours)
                    .replaceAll("MM", padToLen("" + (minutes % 60), '0', 2))
                    .replaceAll("ss", padToLen("" + (seconds % 60), '0', 2))
            );

            if (!newUpdatedPosted) {
                timerHandler.postDelayed(() -> {
                    newUpdatedPosted = false;
                    if (!isClosed) {
                        updateTimerTime();
                    }
                }, 500);
                newUpdatedPosted = true;
            }
        }
    }

    public TimerListViewHolder(
        BaseFragment baseFragment,
        QuickTimerEntryBinding binding,
        @NotNull TimerListViewHolderCallback callbacks
    ) {
        super(binding.getRoot());

        this.baseFragment = baseFragment;
        this.binding = binding;
        this.callbacks = callbacks;

        credStore = baseFragment.getMainActivity().getCredStore();
        client = baseFragment.getMainActivity().getClient();

        binding.currentTimerTime.setText("");
        timerHandler = new Handler(baseFragment.getMainActivity().getMainLooper());

        binding.appTimerDefaultType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                credStore.setTimerDefaultSelection(timer.id, (int) l);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        startStopLogic = new SwitchButtonLogic(
            binding.appStartTimerButton,
            binding.appStopTimerButton,
            false
        );
        startStopLogic.addStateListener(
            (active, userClicked) -> {
                if (timer == null) {
                    return;
                }
                if (!userClicked) {
                    return;
                }

                if (active) {
                    client.setTimerActive(timer.id, true, new BabyBuddyClient.RequestCallback<Boolean>() {
                        @Override
                        public void error(Exception error) {
                        }

                        @Override
                        public void response(Boolean response) {
                            timer.active = true;
                            timer.start = new Date(System.currentTimeMillis() + client.getServerDateOffsetMillis());
                            updateActiveState();
                        }
                    });
                } else {
                    storeActivity();
                }
            }
        );

        notesEditorSwitch = new SwitchButtonLogic(
            binding.addNoteButton, binding.removeNoteButton, false
        );
        NotesEditorBinding notesBinding = NotesEditorBinding.inflate(
            baseFragment.getMainActivity().getLayoutInflater()
        );
        binding.verticalRoot.addView(notesBinding.getRoot());
        notesEditor = new NotesEditorLogic(
            baseFragment.getMainActivity(), notesBinding, false
        );
        notesEditorSwitch.addStateListener((v, userTriggered) -> notesEditor.setVisible(v));
    }

    class StoreActivityCallback implements BabyBuddyClient.RequestCallback<Boolean> {
        private final String errorMessage;

        public StoreActivityCallback(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public void error(@NotNull Exception error) {
            String message = errorMessage;
            if (error instanceof RequestCodeFailure) {
                final RequestCodeFailure rcf = (RequestCodeFailure) error;
                if (rcf.hasJSONMessage()) {
                    message = Phrase.from(baseFragment.getResources(), R.string.activity_store_failure_server_error)
                        .put("message", errorMessage)
                        .put("server_message", String.join(", ", rcf.jsonErrorMessages()))
                        .format().toString();
                }
            }

            baseFragment.showQuestion(
                true,
                baseFragment.getString(R.string.activity_store_failure_message),
                message,
                baseFragment.getString(R.string.activity_store_failure_cancel),
                baseFragment.getString(R.string.activity_store_failure_stop_timer),
                b -> {
                    if (!b) {
                        client.setTimerActive(timer.id, false, new BabyBuddyClient.RequestCallback<Boolean>() {
                            @Override
                            public void error(@NonNull Exception error) {
                                baseFragment.showError(
                                    true,
                                    R.string.activity_store_failure_failed_to_stop_title,
                                    R.string.activity_store_failure_failed_to_stop_message
                                );
                                updateActiveState();
                            }

                            @Override
                            public void response(Boolean response) {
                                updateActiveState();
                            }
                        });
                    } else {
                        updateActiveState();
                    }
                }
            );


            updateActiveState();
        }

        @Override
        public void response(Boolean response) {
            timer.active = false;
            updateActiveState();

            notesEditor.clearText();
            notesEditorSwitch.setState(false);
        }
    }

    private interface BareStoreFunction {
        public void doStore(
            @NonNull BabyBuddyClient.RequestCallback<Boolean> callback
        );
    }

    private class TimerStoreFunction implements StoreFunction<Boolean> {
        private final int selectedActivity;
        private final BareStoreFunction bareStoreFunction;
        private final StoreActivityCallback sac;
        private final BabyBuddyClient.Timer timer;

        public TimerStoreFunction(
            StoreActivityCallback _sac,
            BabyBuddyClient.Timer _timer,
            int _selectedActivity,
            BareStoreFunction _bareStoreFunction
        ) {
            timer = _timer;
            selectedActivity = _selectedActivity;
            bareStoreFunction = _bareStoreFunction;
            sac = _sac;
        }

        @Override
        public void store(
            @NonNull BabyBuddyClient.Timer _timer,
            @NonNull BabyBuddyClient.RequestCallback<java.lang.Boolean> callback
        ) {
            bareStoreFunction.doStore(callback);
        }

        @NonNull
        @Override
        public String name() {
            return BabyBuddyClient.ACTIVITIES.ALL[selectedActivity];
        }

        @Override
        public void error(@NotNull Exception error) {
            sac.error(error);
            updateActiveState();
        }

        @Override
        public void response(java.lang.Boolean response) {
            sac.response(response);
            updateActiveState();
            callbacks.updateActivities();
        }

        @Override
        public void timerStopped() {
            updateActiveState();
        }

        @Override
        public void cancel() {
            timer.active = true;
            updateActiveState();
        }
    }

    private void storeActivity() {
        final int selectedActivity = (int) binding.appTimerDefaultType.getSelectedItemId();
        final MainActivity mainActivity = this.baseFragment.getMainActivity();
        final String[] timerTypeStrings = baseFragment.getResources().getStringArray(R.array.timerTypes);
        final StoreActivityCallback sac = new StoreActivityCallback(
            Phrase.from("Storing {name} entry failed.")
                .putOptional("name", timerTypeStrings[selectedActivity])
                .format()
                .toString()
        );

        BareStoreFunction storeFunction = null;
        if (selectedActivity == 0) {
            baseFragment.getMainActivity().selectedTimer = timer;
            Navigation.findNavController(baseFragment.getView()).navigate(R.id.action_loggedInFragment2_to_feedingFragment);
        } else if (selectedActivity == 1) {
            storeFunction = callback -> client.createSleepRecordFromTimer(
                timer,
                notesEditor.getText(),
                callback
            );
        } else if (selectedActivity == 2) {
            storeFunction = callback -> client.createTummyTimeRecordFromTimer(
                timer,
                notesEditor.getText(),
                callback
            );
        } else {
            baseFragment.showError(
                true,
                R.string.error_storing_activity_failed_title,
                R.string.error_storing_activity_unsupported
            );
            return;
        }

        if (storeFunction != null) {
            mainActivity.storeActivity(timer, new TimerStoreFunction(
                sac,
                timer,
                selectedActivity,
                storeFunction
            ));
        }
    }

    private void updateActiveState() {
        startStopLogic.setState(timer.active);
        if ((timer == null) || (!timer.active)) {
            timerStartTime = null;
        } else {
            timerStartTime = new Date().getTime() - timer.start.getTime() + client.getServerDateOffsetMillis() - System.currentTimeMillis();
        }
        updateTimerTime();
    }

    private int inferDefaultSelectionFromName(String name) {
        name = name.toLowerCase();
        int i = 0;
        for (CharSequence candidate : baseFragment.getActivity().getResources().getStringArray(R.array.timerTypes)) {
            if (name.contains(candidate.toString().toLowerCase())) {
                return i;
            }
            i++;
        }
        return 0;
    }

    public void assignTimer(BabyBuddyClient.Timer timer) {
        if (isClosed) {
            isClosed = false;
            updateTimerTime();
        }

        this.timer = timer;
        binding.timerName.setText(timer.readableName());
        Integer defaultSelection = credStore.getTimerDefaultSelections().get(timer.id);
        if (defaultSelection == null) {
            if (timer.name != null) {
                defaultSelection = inferDefaultSelectionFromName(timer.name);
            } else {
                defaultSelection = 0;
            }
        }
        binding.appTimerDefaultType.setSelection(defaultSelection);
        updateActiveState();

        notesEditor.setIdentifier("timer_" + timer.id);
        notesEditorSwitch.setState(notesEditor.isVisible());
    }

    public BabyBuddyClient.Timer getTimer() {
        return timer.clone();
    }

    public void close() {
        timerStartTime = null;
        isClosed = true;
    }
}

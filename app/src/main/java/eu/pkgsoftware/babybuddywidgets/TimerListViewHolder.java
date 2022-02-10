package eu.pkgsoftware.babybuddywidgets;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;

import java.util.Date;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.widgets.SwitchButtonLogic;

public class TimerListViewHolder extends RecyclerView.ViewHolder {
    private final QuickTimerEntryBinding binding;

    private final BaseFragment baseFragment;
    private final CredStore credStore;
    private final BabyBuddyClient client;
    private final Handler timerHandler;

    private final SwitchButtonLogic notesEditorSwitch;
    private final NotesEditorLogic notesEditor;

    private SwitchButtonLogic startStopLogic = null;

    private BabyBuddyClient.Timer timer = null;
    private long timerStartTime = -1;

    private String padToLen(String s, char c, int length) {
        StringBuilder sBuilder = new StringBuilder(s);
        while (sBuilder.length() < length) {
            sBuilder.insert(0, c);
        }
        return sBuilder.toString()  ;
    }

    private boolean newUpdatedPosted = false;
    private void updateTimerTime() {
        if (timerStartTime < 0) {
            binding.currentTimerTime.setText("");
        } else {
            long diff = System.currentTimeMillis() - timerStartTime;

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
                    updateTimerTime();
                }, 500);
                newUpdatedPosted = true;
            }
        }
    }

    public TimerListViewHolder(BaseFragment baseFragment, QuickTimerEntryBinding binding) {
        super(binding.getRoot());

        this.baseFragment = baseFragment;
        this.binding = binding;

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
        public void error(Exception error) {
            baseFragment.showError(true, "Could not store activity", errorMessage);
        }

        @Override
        public void response(Boolean response) {
            timer.active = false;
            updateActiveState();

            notesEditor.clearText();
            notesEditorSwitch.setState(false);
        }
    }

    private void storeActivity() {
        int selectedActivity = (int) binding.appTimerDefaultType.getSelectedItemId();
        if ((int) binding.appTimerDefaultType.getSelectedItemId() == 0) {
            baseFragment.getMainActivity().selectedTimer = timer;
            Navigation.findNavController(baseFragment.getView()).navigate(R.id.action_loggedInFragment2_to_feedingFragment);
        } else if (selectedActivity == 1) {
            client.createSleepRecordFromTimer(
                timer,
                notesEditor.getText(),
                new StoreActivityCallback("Storing Sleep Time failed.")
            );
        } else if (selectedActivity == 2) {
            client.createTummyTimeRecordFromTimer(
                timer,
                notesEditor.getText(),
                new StoreActivityCallback("Storing Tummy Time failed.")
            );
        } else {
            baseFragment.showError(true, "Could not store activity", "Unsupported activity");
        }
    }

    private void updateActiveState() {
        startStopLogic.setState(timer.active);
        if ((timer == null) || (!timer.active)) {
            timerStartTime = -1;
        } else {
            timerStartTime = Math.max(0, timer.start.getTime() - client.getServerDateOffsetMillis());
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
}

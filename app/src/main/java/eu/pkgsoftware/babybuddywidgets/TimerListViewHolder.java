package eu.pkgsoftware.babybuddywidgets;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;

import java.util.Date;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;

public class TimerListViewHolder extends RecyclerView.ViewHolder {
    private final QuickTimerEntryBinding binding;

    private final BaseFragment baseFragment;
    private final CredStore credStore;
    private final BabyBuddyClient client;
    private final Handler timerHandler;

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
        binding.appStartTimerButton.setOnClickListener(
            view -> client.setTimerActive(timer.id, true, new BabyBuddyClient.RequestCallback<Boolean>() {
                @Override
                public void error(Exception error) {
                }

                @Override
                public void response(Boolean response) {
                    timer.active = true;
                    timer.start = new Date(System.currentTimeMillis());
                    updateActiveState();
                }
            }
        ));
        binding.appStopTimerButton.setOnClickListener(new View.OnClickListener() {
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
                }
            }

            @Override
            public void onClick(View view) {
                if ((int) binding.appTimerDefaultType.getSelectedItemId() == 0) {
                    baseFragment.getMainActivity().selectedTimer = timer;
                    Navigation.findNavController(baseFragment.getView()).navigate(R.id.action_loggedInFragment2_to_feedingFragment);
                } else {
                    client.setTimerActive(timer.id, false, new BabyBuddyClient.RequestCallback<Boolean>() {
                        @Override
                        public void error(Exception error) {
                        }

                        @Override
                        public void response(Boolean response) {
                            timer.active = false;
                            updateActiveState();
                            storeActivity();
                        }
                    });
                }
            }

            private void storeActivity() {
                int selectedActivity = (int) binding.appTimerDefaultType.getSelectedItemId();
                if (selectedActivity == 1) {
                    client.createSleepRecordFromTimer(
                        timer,
                        new StoreActivityCallback("Storing Sleep Time failed.")
                    );
                } else if (selectedActivity == 2) {
                    client.createSleepRecordFromTimer(
                        timer,
                        new StoreActivityCallback("Storing Tummy Time failed.")
                    );
                    client.createTummyTimeRecordFromTimer(timer, new BabyBuddyClient.RequestCallback<Boolean>() {
                        @Override
                        public void error(Exception error) {
                            baseFragment.showError(true, "Could not store activity", error.getMessage());
                        }

                        @Override
                        public void response(Boolean response) {
                            // all done :)
                        }
                    });
                } else {
                    baseFragment.showError(true, "Could not store activity", "Unsupported activity");
                }
            }
        });
    }

    public void updateActiveState() {
        binding.appStartTimerButton.setVisibility(timer.active ? View.GONE : View.VISIBLE);
        binding.appStopTimerButton.setVisibility(!timer.active ? View.GONE : View.VISIBLE);

        if ((timer == null) || (!timer.active)) {
            timerStartTime = -1;
        } else {
            timerStartTime = Math.max(0, timer.start.getTime());
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
        updateActiveState();
        Integer defaultSelection = credStore.getTimerDefaultSelections().get(timer.id);
        if (defaultSelection == null) {
            if (timer.name != null) {
                defaultSelection = inferDefaultSelectionFromName(timer.name);
            } else {
                defaultSelection = 0;
            }
        }
        binding.appTimerDefaultType.setSelection(defaultSelection);
    }
}

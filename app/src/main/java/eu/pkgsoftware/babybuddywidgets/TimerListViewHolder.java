package eu.pkgsoftware.babybuddywidgets;

import android.view.View;
import android.widget.AdapterView;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;

public class TimerListViewHolder extends RecyclerView.ViewHolder {
    private BaseFragment baseFragment;

    private QuickTimerEntryBinding binding;
    private BabyBuddyClient.Timer timer = null;

    private CredStore credStore;
    private BabyBuddyClient client;

    public TimerListViewHolder(BaseFragment baseFragment, QuickTimerEntryBinding binding) {
        super(binding.getRoot());

        this.baseFragment = baseFragment;
        this.binding = binding;

        credStore = baseFragment.getMainActivity().getCredStore();
        client = baseFragment.getMainActivity().getClient();

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
                        updateActiveState();
                    }
                }
            ));
        binding.appStopTimerButton.setOnClickListener(new View.OnClickListener() {
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

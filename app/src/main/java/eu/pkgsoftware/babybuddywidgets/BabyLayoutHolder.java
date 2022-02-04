package eu.pkgsoftware.babybuddywidgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;

public class BabyLayoutHolder extends RecyclerView.ViewHolder {
    private class TimerListViewHolder extends RecyclerView.ViewHolder {
        public QuickTimerEntryBinding binding;
        public BabyBuddyClient.Timer timer = null;

        public TimerListViewHolder(View itemView, QuickTimerEntryBinding binding) {
            super(itemView);
            this.binding = binding;

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

    private class TimerListProvider extends RecyclerView.Adapter<TimerListViewHolder> {
        private BabyBuddyClient.Timer[] timers;

        public TimerListProvider(BabyBuddyClient.Timer[] timers) {
            this.timers = timers;
        }

        @Override
        public TimerListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            QuickTimerEntryBinding entryBinding = QuickTimerEntryBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new TimerListViewHolder(entryBinding.getRoot(), entryBinding);
        }

        @Override
        public void onBindViewHolder(TimerListViewHolder holder, int position) {
            holder.assignTimer(timers[position]);
        }

        @Override
        public int getItemCount() {
            return timers.length;
        }
    }

    private BabyManagerBinding binding;
    private final BaseFragment baseFragment;
    private final BabyBuddyClient client;
    private final CredStore credStore;

    private BabyBuddyClient.Child child = null;

    private boolean changeWet = false;
    private boolean changeSolid = false;

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
        binding.createDefaultTimers.setOnClickListener(view -> createDefaultTimers());
    }

    private void resetDiaperUi() {
        changeSolid = false;
        changeWet = false;
        updateDiaperBar();
    }

    private void updateDiaperBar() {
        binding.sendChangeButton.setVisibility((changeSolid || changeWet) ? View.VISIBLE : View.GONE);
        binding.solidEnabledButton.setVisibility(changeSolid ? View.VISIBLE : View.GONE);
        binding.solidDisabledButton.setVisibility(!changeSolid ? View.VISIBLE : View.GONE);
        binding.wetEnabledButton.setVisibility(changeWet ? View.VISIBLE : View.GONE);
        binding.wetDisabledButton.setVisibility(!changeWet ? View.VISIBLE : View.GONE);
    }

    private void storeDiaperChange() {
        client.createChangeRecord(child, changeWet, changeSolid,
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
                    // Pass
                }
            }
        );

        resetDiaperUi();
    }

    private void createDefaultTimers() {
        int i = 0;
        for (String timerTypeName : baseFragment.getResources().getStringArray(R.array.timerTypes)) {
            final int finalI = i;
            client.createTimer(child, timerTypeName, new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer>() {
                @Override
                public void error(Exception error) {
                }

                @Override
                public void response(BabyBuddyClient.Timer response) {
                    credStore.setTimerDefaultSelection(response.id, finalI);
                    client.setTimerActive(response.id, false, new BabyBuddyClient.RequestCallback<Boolean>() {
                            @Override
                            public void error(Exception error) {
                            }

                            @Override
                            public void response(Boolean response) {
                            }
                        }
                    );

                    RecyclerView.Adapter<?> a = binding.timersList.getAdapter();
                    for (int i = 0; i < a.getItemCount(); i++) {
                        a.notifyItemChanged(i);
                    }
                }
            });
            i++;
        }
    }

    public void updateChild(BabyBuddyClient.Child c) {
        this.child = c;
        resetDiaperUi();
    }

    public void updateTimerList(BabyBuddyClient.Timer[] timers) {
        binding.timersList.setAdapter(new BabyLayoutHolder.TimerListProvider(timers));
        binding.createDefaultTimers.setVisibility(timers.length == 0 ? View.VISIBLE : View.GONE);
    }

    public void onViewSelected() {
        resetDiaperUi();
        binding.createDefaultTimers.setVisibility(View.GONE);
    }

    public void onViewDeselected() {
        resetDiaperUi();
    }
}


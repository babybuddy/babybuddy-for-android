package eu.pkgsoftware.babybuddywidgets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.net.ConnectException;
import java.util.Timer;
import java.util.TimerTask;

import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;

public class LoggedInFragment extends BaseFragment {
    private LoggedInFragmentBinding binding;

    private BabyBuddyClient client = null;
    private CredStore credStore = null;

    private BabyBuddyClient.Child selectedChild = null;

    private boolean changeWet = false;
    private boolean changeSolid = false;

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

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
            binding.appStartTimerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    client.setTimerActive(timer.id, true, new BabyBuddyClient.RequestCallback<Boolean>() {
                        @Override
                        public void error(Exception error) {
                        }

                        @Override
                        public void response(Boolean response) {
                            timer.active = true;
                            updateActiveState();
                        }
                    });
                }
            });
            binding.appStopTimerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ((int) binding.appTimerDefaultType.getSelectedItemId() == 0) {
                        getMainActivity().selectedTimer = timer;
                        Navigation.findNavController(getView()).navigate(R.id.action_loggedInFragment2_to_feedingFragment);
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
                        showError(false, "Could not store activity", errorMessage);
                    }

                    @Override
                    public void response(Boolean response) {
                    }
                }

                private void storeActivity() {
                    int selectedActivity = (int) binding.appTimerDefaultType.getSelectedItemId();
                    if (selectedActivity == 1) {
                        client.createSleepRecordFromTimer(timer, new StoreActivityCallback("Storing Sleep Time failed."));
                    } else if (selectedActivity == 2) {
                        client.createSleepRecordFromTimer(timer, new StoreActivityCallback("Storing Tummy Time failed."));
                        client.createTummyTimeRecordFromTimer(timer, new BabyBuddyClient.RequestCallback<Boolean>() {
                            @Override
                            public void error(Exception error) {
                                showError(false, "Could not store activity", error.getMessage());
                            }

                            @Override
                            public void response(Boolean response) {
                                // all done :)
                            }
                        });
                    } else {
                        showError(false, "Could not store activity", "Unsupported activity");
                    }
                }
            });
        }

        public void updateActiveState() {
            binding.appStartTimerButton.setVisibility(timer.active ? View.GONE : View.VISIBLE);
            binding.appStopTimerButton.setVisibility(!timer.active ? View.GONE : View.VISIBLE);
            refreshTimerList();
        }

        public void assignTimer(BabyBuddyClient.Timer timer) {
            this.timer = timer;
            binding.timerName.setText(timer.readableName());
            updateActiveState();
            Integer defaultSelection = credStore.getTimerDefaultSelections().get(timer.id);
            if (defaultSelection == null) {
                defaultSelection = 0;
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
        public void onBindViewHolder(LoggedInFragment.TimerListViewHolder holder, int position) {
            holder.assignTimer(timers[position]);
        }

        @Override
        public int getItemCount() {
            return timers.length;
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = LoggedInFragmentBinding.inflate(inflater, container, false);

        credStore = getMainActivity().getCredStore();
        client = getMainActivity().getClient();

        GridLayoutManager l = new GridLayoutManager(binding.timersList.getContext(), 1);
        binding.timersList.setLayoutManager(l);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshTimerList();
            }
        }, 0, 1000);

        setHasOptionsMenu(true);

        View.OnClickListener invertSolid = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeSolid = !changeSolid;
                updateDiaperBar();
            }
        };
        binding.solidEnabledButton.setOnClickListener(invertSolid);
        binding.solidDisabledButton.setOnClickListener(invertSolid);

        View.OnClickListener invertWet = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeWet = !changeWet;
                updateDiaperBar();
            }
        };
        binding.wetEnabledButton.setOnClickListener(invertWet);
        binding.wetDisabledButton.setOnClickListener(invertWet);
        binding.sendChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storeDiaperChange();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.loggedin_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logoutMenuButton) {
            getMainActivity().getCredStore().storeAppToken(null);
            Navigation.findNavController(getView()).navigate(R.id.logoutOperation);
        }
        return false;
    }

    private boolean timerListRefreshing = false;
    private BabyBuddyClient.Timer[] oldTimerList = new BabyBuddyClient.Timer[0];
    private void refreshTimerList() {
        if (timerListRefreshing) {
            return;
        }

        timerListRefreshing = true;
        client.listTimers(new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
            @Override
            public void error(Exception error) {
                timerListRefreshing = false;
                if (!isVisible()) {
                    return;
                }

                // Connect exceptions happen regularly... we just ignore them
                if (ConnectException.class.isInstance(error)) return;

                showError(
                    false,
                    "Login failed",
                    "Error occurred while obtaining timers: " + error.getMessage()
                );
            }

            @Override
            public void response(BabyBuddyClient.Timer[] response) {
                timerListRefreshing = false;
                if (!isVisible()) {
                    return;
                }

                boolean changed = false;
                if (!changed) {
                    changed = oldTimerList.length != response.length;
                }
                if (!changed) {
                    for (int i = 0; i < oldTimerList.length; i++) {
                        changed = (oldTimerList[i].id != response[i].id) ||
                                (oldTimerList[i].active != response[i].active) ||
                                (!oldTimerList[i].start.equals(response[i].start));
                        if (changed) break;
                    }
                }

                if (changed) {
                    oldTimerList = response;
                    binding.timersList.setAdapter(new TimerListProvider(response));
                }
            }
        });
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        oldTimerList = new BabyBuddyClient.Timer[0];
        refreshTimerList();

        selectedChild = null;
        String selectedChildName = credStore.getSelectedChild();
        for (BabyBuddyClient.Child c : getMainActivity().children) {
            if (c.slug.equals(selectedChildName)) {
                selectedChild = c;
                break;
            }
        }
        if (selectedChild == null) {
            if (getMainActivity().children.length > 0) {
                selectedChild = getMainActivity().children[0];
            }
        }

        credStore.setSelectedChild(selectedChild != null ? selectedChild.slug : null);
        updateTitle();

        resetDiaperUi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void resetDiaperUi() {
        changeSolid = false;
        changeWet = false;
        updateDiaperBar();
    }

    private void updateTitle() {
        if (selectedChild == null) {
            getMainActivity().setTitle("(No children found)");
        } else {
            getMainActivity().setTitle(selectedChild.first_name + " " + selectedChild.last_name);
        }
    }

    private void updateDiaperBar() {
        binding.sendChangeButton.setVisibility((changeSolid || changeWet) ? View.VISIBLE : View.GONE);
        binding.solidEnabledButton.setVisibility(changeSolid ? View.VISIBLE : View.GONE);
        binding.solidDisabledButton.setVisibility(!changeSolid ? View.VISIBLE : View.GONE);
        binding.wetEnabledButton.setVisibility(changeWet ? View.VISIBLE : View.GONE);
        binding.wetDisabledButton.setVisibility(!changeWet ? View.VISIBLE : View.GONE);
    }

    private void storeDiaperChange() {
        client.createChangeRecord(selectedChild, changeWet, changeSolid,
            new BabyBuddyClient.RequestCallback<Boolean>() {
                @Override
                public void error(Exception error) {
                    showError(
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
}

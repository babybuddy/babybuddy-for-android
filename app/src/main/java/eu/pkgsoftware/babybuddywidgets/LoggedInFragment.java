package eu.pkgsoftware.babybuddywidgets;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridLayout;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;

public class LoggedInFragment extends Fragment {
    private LoggedInFragmentBinding binding;

    private BabyBuddyClient client = null;
    private CredStore credStore = null;

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

                private void reportError(String message) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Could not store activity")
                            .setMessage(message)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .show();
                }

                class StoreActivityCallback implements BabyBuddyClient.RequestCallback<Boolean> {
                    private final String errorMessage;

                    public StoreActivityCallback(String errorMessage) {
                        this.errorMessage = errorMessage;
                    }

                    @Override
                    public void error(Exception error) {
                        reportError(errorMessage);
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
                                reportError(error.getMessage());
                            }

                            @Override
                            public void response(Boolean response) {
                                // all done :)
                            }
                        });
                    } else {
                        reportError("Unsupported activity");
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

        credStore = new CredStore(getContext());
        client = new BabyBuddyClient(getActivity().getMainLooper(), credStore);

        GridLayoutManager l = new GridLayoutManager(binding.timersList.getContext(), 1);
        binding.timersList.setLayoutManager(l);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshTimerList();
            }
        }, 0, 1000);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        oldTimerList = new BabyBuddyClient.Timer[0];
        refreshTimerList();
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

                new AlertDialog.Builder(getContext())
                        .setTitle("Login failed")
                        .setMessage("Error occurred while obtaining timers: " + error.getMessage())
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                timerListRefreshing = false;
                                dialogInterface.cancel();
                            }
                        })
                        .show();
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

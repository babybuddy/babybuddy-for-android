package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.lang.reflect.Array;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;

public class LoggedInFragment extends BaseFragment {
    private LoggedInFragmentBinding binding;

    private BabyBuddyClient client = null;
    private CredStore credStore = null;

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    private BabyPagerAdapter babyAdapter = null;

    private class BabyLayoutHolder extends RecyclerView.ViewHolder {
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
            public void onBindViewHolder(TimerListViewHolder holder, int position) {
                holder.assignTimer(timers[position]);
            }

            @Override
            public int getItemCount() {
                return timers.length;
            }
        }

        public BabyManagerBinding binding;

        private BabyBuddyClient.Child child = null;

        private boolean changeWet = false;
        private boolean changeSolid = false;

        private boolean timerListRefreshing = false;
        private BabyBuddyClient.Timer[] oldTimerList = new BabyBuddyClient.Timer[0];

        public BabyLayoutHolder(BabyManagerBinding bmb) {
            super(bmb.getRoot());
            binding = bmb;

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
        }

        public void refreshTimerList() {
            if (timerListRefreshing) {
                return;
            }
            if (child == null) {
                return;
            }

            timerListRefreshing = true;
            client.listTimers(child.id, new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
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

        public void updateChild(BabyBuddyClient.Child c) {
            this.child = c;
            resetDiaperUi();
        }

        public void onViewSelected() {
            resetDiaperUi();
        }

        public void onViewDeselected() {
            resetDiaperUi();
        }
    }

    private class BabyPagerAdapter extends RecyclerView.Adapter<BabyLayoutHolder> {
        private ArrayList<BabyLayoutHolder> holders = new ArrayList<>();

        @Override
        public BabyLayoutHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            BabyManagerBinding babyBinding = BabyManagerBinding.inflate(
                getLayoutInflater(), null, false
            );
            View v = babyBinding.getRoot();
            v.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT
            ));
            return new BabyLayoutHolder(babyBinding);
        }

        @Override
        public void onBindViewHolder(BabyLayoutHolder holder, int position) {
            while (position >= holders.size()) holders.add(null);
            holders.set(position, holder);
            holder.updateChild(getMainActivity().children[position]);
        }

        @Override
        public int getItemCount() {
            return getMainActivity().children.length;
        }

        public void activeViewChanged(int position) {
            for (int i = 0; i < holders.size(); i++) {
                BabyLayoutHolder holder = holders.get(position);
                if (holder == null) continue;
                if (i == position) {
                    holder.onViewSelected();
                } else {
                    holder.onViewDeselected();
                }
            }
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

        babyAdapter = new BabyPagerAdapter();
        binding.babyViewPagerSwitcher.setAdapter(babyAdapter);
        binding.babyViewPagerSwitcher.registerOnPageChangeCallback(
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    babyAdapter.activeViewChanged(position);
                    updateTitle();
                }
            }
        );
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

    @Override
    public void onResume() {
        super.onResume();

        System.out.println("RESTORE BELOW");
        /*
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
        */
        updateTitle();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private BabyBuddyClient.Child selectedChild() {
        int childIndex = binding.babyViewPagerSwitcher.getCurrentItem();
        BabyBuddyClient.Child child = null;
        if ((childIndex >= 0) && (childIndex < getMainActivity().children.length)) {
            child = getMainActivity().children[childIndex];
        }
        return child;
    }

    private void updateTitle() {
        BabyBuddyClient.Child child = selectedChild();
        if (child == null) {
            getMainActivity().setTitle("(No children found)");
        } else {
            getMainActivity().setTitle(child.first_name + " " + child.last_name);
        }
    }
}

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
    private class TimerListProvider extends RecyclerView.Adapter<TimerListViewHolder> {
        private BabyBuddyClient.Timer[] timers;

        public TimerListProvider(BabyBuddyClient.Timer[] timers) {
            this.timers = timers;
        }

        @Override
        public TimerListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            QuickTimerEntryBinding entryBinding = QuickTimerEntryBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new TimerListViewHolder(baseFragment, entryBinding);
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


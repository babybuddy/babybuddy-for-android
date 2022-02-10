package eu.pkgsoftware.babybuddywidgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.widgets.SwitchButtonLogic;

public class BabyLayoutHolder extends RecyclerView.ViewHolder {
    private class TimerListProvider extends RecyclerView.Adapter<TimerListViewHolder> {
        private BabyBuddyClient.Timer[] timers = new BabyBuddyClient.Timer[0];
        private List<TimerListViewHolder> holders = new LinkedList<>();

        private int[] listIds(BabyBuddyClient.Timer[] t) {
            int[] result = new int[t.length];
            for (int i = 0; i < t.length; i++) {
                result[i] = t[i].id;
            }
            Arrays.sort(result);
            return result;
        }

        private boolean compareIds(BabyBuddyClient.Timer[] t1, BabyBuddyClient.Timer[] t2) {
            return Arrays.equals(listIds(t1), listIds(t2));
        }

        private TimerListViewHolder findHolderForTimer(BabyBuddyClient.Timer t) {
            TimerListViewHolder result = null;
            for (TimerListViewHolder h: holders) {
                if (h.getTimer().id == t.id) {
                    if (result != null) {
                        return null; // Multiple timers - dismiss
                    } else {
                        result = h;
                    }
                }
            }
            return result;
        }

        public void updateTimers(BabyBuddyClient.Timer[] timers) {
            timers = timers.clone();
            Arrays.sort(timers, (a, b) -> Integer.compare(a.id, b.id));

            if (!compareIds(timers, this.timers)) {
                this.timers = timers;
                notifyDataSetChanged();
            } else {
                for (int i = 0; i < timers.length; i++) {
                    if (!this.timers[i].equals(timers[i])) {
                        BabyBuddyClient.Timer probeTimer = timers[i].clone();
                        probeTimer.start = this.timers[i].start;
                        probeTimer.end = this.timers[i].end;
                        TimerListViewHolder timerHolder = findHolderForTimer(timers[i]);
                        boolean probeTimerEqual = probeTimer.equals(this.timers[i]);

                        this.timers[i] = timers[i];
                        if (probeTimerEqual && (timerHolder != null)) {
                            timerHolder.assignTimer(timers[i]);
                        } else {
                            notifyItemChanged(i);
                        }
                    }
                }
            }
        }

        @Override
        public TimerListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            QuickTimerEntryBinding entryBinding = QuickTimerEntryBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new TimerListViewHolder(baseFragment, entryBinding);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull TimerListViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            holders.add(holder);
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull TimerListViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            holders.remove(holder);
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
    private BabyLayoutHolder.TimerListProvider timerListProvider;

    private BabyBuddyClient.Child child = null;

    private boolean changeWet = false;
    private boolean changeSolid = false;

    private NotesEditorLogic notesEditor;
    private SwitchButtonLogic notesSwitch;

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

        notesSwitch = new SwitchButtonLogic(
            binding.addNoteButton,
            binding.removeNoteButton,
            false
        );

        timerListProvider = new BabyLayoutHolder.TimerListProvider();
        binding.timersList.setAdapter(timerListProvider);

        NotesEditorBinding notesEditorBinding = NotesEditorBinding.inflate(
            fragment.getMainActivity().getLayoutInflater()
        );
        binding.diaperNotesSlot.addView(notesEditorBinding.getRoot());

        notesEditor = new NotesEditorLogic(
            fragment.getMainActivity(),
            notesEditorBinding,
            false
        );
        notesSwitch.addStateListener((b, userInduced) -> notesEditor.setVisible(b));
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
                public void error(Exception error) {
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

        notesEditor.setIdentifier("diaper_" + c.slug);
        notesSwitch.setState(notesEditor.isVisible());

        resetDiaperUi();
    }

    public void updateTimerList(BabyBuddyClient.Timer[] timers) {
        timerListProvider.updateTimers(timers);
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


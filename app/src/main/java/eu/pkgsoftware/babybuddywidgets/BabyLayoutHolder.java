package eu.pkgsoftware.babybuddywidgets;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.NotesEditorBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;
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
            for (TimerListViewHolder h : holders) {
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
    private ChildEventHistoryLoader childHistoryLoader = null;

    private ChildrenStateTracker.ChildObserver childObserver = null;

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

    public BabyBuddyClient.Child getChild() {
        return child;
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

    public void recreateDefaultTimers() {
        removeTimers(this::createDefaultTimers);
    }

    private void removeTimers(final Runnable after) {
        client.listTimers(child.id, new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
            @Override
            public void error(Exception error) {
                baseFragment.showError(
                    true,
                    "Failed to remove timers",
                    "Getting timer list failed."
                );
            }

            @Override
            public void response(final BabyBuddyClient.Timer[] response) {
                Boolean[] removed = new Boolean[response.length];

                for (int i = 0; i < response.length; i++) {
                    final BabyBuddyClient.Timer t = response[i];
                    final int _i = i;
                    client.deleteTimer(t.id, new BabyBuddyClient.RequestCallback<Boolean>() {
                        public boolean allRemoved() {
                            for (Boolean r : removed) {
                                if ((r == null) || !r) {
                                    return false;
                                }
                            }
                            return true;
                        }

                        public boolean anyFailed() {
                            for (Boolean r : removed) {
                                if ((r != null) && !r) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        @Override
                        public void error(Exception error) {
                            if (!anyFailed()) {
                                baseFragment.showError(
                                    true,
                                    "Failed to remove timers",
                                    "Timer could not be deleted"
                                );
                            }
                            removed[_i] = false;
                        }

                        @Override
                        public void response(Boolean response) {
                            removed[_i] = true;

                            if (allRemoved()) {
                                requeueImmediateTimerListRefresh();
                                after.run();
                            }
                        }
                    });
                }
            }
        });
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
                            requeueImmediateTimerListRefresh();
                        }

                        @Override
                        public void response(Boolean response) {
                            requeueImmediateTimerListRefresh();
                        }
                    });
                }
            });
            i++;
        }
    }

    private void requeueImmediateTimerListRefresh() {
        client.listTimers(child.id, new BabyBuddyClient.RequestCallback<BabyBuddyClient.Timer[]>() {
            @Override
            public void error(Exception error) {
            }

            @Override
            public void response(BabyBuddyClient.Timer[] response) {
                updateTimerList(response);
            }
        });
    }

    private void resetChildHistoryLoader() {
        if (childHistoryLoader != null) {
            childHistoryLoader.close();
        }
        childHistoryLoader = null;
    }

    public void updateChild(BabyBuddyClient.Child c) {
        clear();
        this.child = c;
        notesEditor.setIdentifier("diaper_" + c.slug);
        notesSwitch.setState(notesEditor.isVisible());
    }

    public void updateTimerList(BabyBuddyClient.Timer[] timers) {
        for (BabyBuddyClient.Timer t : timers) {
            if ((child == null) || (t.child_id != child.id)) {
                timerListProvider.updateTimers(new BabyBuddyClient.Timer[0]);
                return;
            }
        }

        timerListProvider.updateTimers(timers);
        binding.createDefaultTimers.setVisibility(timers.length == 0 ? View.VISIBLE : View.GONE);
    }

    public void onViewSelected(ChildrenStateTracker stateTracker) {
        binding.createDefaultTimers.setVisibility(View.GONE);

        resetChildObserver();
        resetChildHistoryLoader();
        resetDiaperUi();

        if (child != null) {
            System.out.println("AAA CREATE " + child.slug);
            childObserver = stateTracker.new ChildObserver(child.id, new ChildrenStateTracker.ChildListener() {
                @Override
                public void childValidUpdated(boolean valid) {
                }

                @Override
                public void timersUpdated(BabyBuddyClient.Timer[] timers) {
                    updateTimerList(timers);
                }
            });

            childHistoryLoader = new ChildEventHistoryLoader(baseFragment, binding.timeline, child.id);
            childHistoryLoader.createTimelineObserver(stateTracker);
        }
    }

    public void onViewDeselected() {
        resetChildObserver();
        resetChildHistoryLoader();
        resetDiaperUi();
    }

    private void resetChildObserver() {
        if (childObserver != null) {
            System.out.println("AAA CLOSE " + child.slug);
            childObserver.close();
            childObserver = null;
        }
    }

    public void clear() {
        resetChildObserver();
        resetChildHistoryLoader();
        resetDiaperUi();
        child = null;
    }
}


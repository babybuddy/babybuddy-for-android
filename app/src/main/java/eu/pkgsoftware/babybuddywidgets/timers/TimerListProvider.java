package eu.pkgsoftware.babybuddywidgets.timers;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.BaseFragment;
import eu.pkgsoftware.babybuddywidgets.history.ChildEventHistoryLoader;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;

public class TimerListProvider extends RecyclerView.Adapter<TimerListViewHolder> implements TimerListViewHolderCallback {
    private BabyBuddyClient.Timer[] timers = new BabyBuddyClient.Timer[0];

    private final BaseFragment baseFragment;
    private final ChildEventHistoryLoader childHistoryLoader;
    private final List<TimerListViewHolder> holders = new LinkedList<>();
    private final TimerControlInterface timerControls;

    public TimerListProvider(
        @NotNull BaseFragment baseFragment,
        @NonNull ChildEventHistoryLoader childHistoryLoader,
        @NotNull TimerControlInterface timerControls
    ) {
        super();
        this.baseFragment = baseFragment;
        this.childHistoryLoader = childHistoryLoader;
        this.timerControls = timerControls;
    }

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
        eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding entryBinding = eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new TimerListViewHolder(baseFragment, entryBinding, timerControls, this);
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

    public void close() {
        for (TimerListViewHolder h : holders) {
            h.close();
        }
    }

    @Override
    public void updateActivities() {
        childHistoryLoader.forceRefresh();
    }
}

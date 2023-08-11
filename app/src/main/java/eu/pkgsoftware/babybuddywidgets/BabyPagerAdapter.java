package eu.pkgsoftware.babybuddywidgets;

import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;

// I need to extract this one!!!
class BabyPagerAdapter extends RecyclerView.Adapter<BabyLayoutHolder> {
    private List<BabyLayoutHolder> holders = new ArrayList<>();
    private BabyLayoutHolder activeHolder = null;

    private BaseFragment fragment = null;
    private BabyBuddyClient.Child[] children = null;
    private ChildrenStateTracker stateTracker = null;

    public void postInit(
        BaseFragment fragment,
        BabyBuddyClient.Child[] children,
        ChildrenStateTracker stateTracker
    ) {
        this.fragment = fragment;
        this.stateTracker = stateTracker;
        updateChildren(children);
    }

    public void updateChildren(BabyBuddyClient.Child[] children) {
        this.children = children;
        notifyDataSetChanged();
    }

    @Override
    public @NonNull
    BabyLayoutHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BabyManagerBinding babyBinding = BabyManagerBinding.inflate(
            fragment.getLayoutInflater(), null, false
        );
        View v = babyBinding.getRoot();
        v.setLayoutParams(new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        BabyLayoutHolder holder = new BabyLayoutHolder(fragment, babyBinding);
        holders.add(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull BabyLayoutHolder holder, int position) {
        holder.updateChild(children[position], stateTracker);

        int childIndex = LoggedInFragment.childIndexBySlug(
            children,
            fragment.getMainActivity().getCredStore().getSelectedChild()
        );
        if (childIndex >= 0) {
            if (Objects.equals(children[position], children[childIndex])) {
                activeViewChanged(children[position]);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull BabyLayoutHolder holder) {
        holder.clear();
    }

    @Override
    public int getItemCount() {
        if (children == null) {
            return 0;
        }
        return children.length;
    }

    public void activeViewChanged(BabyBuddyClient.Child c) {
        activeHolder = null;
        for (BabyLayoutHolder h : holders) {
            if (Objects.equals(c, h.getChild())) {
                h.updateChild(c, stateTracker);
                activeHolder = h;
            } else {
                h.onViewDeselected();
            }
        }
    }

    public BabyLayoutHolder getActive() {
        return activeHolder;
    }

    public void close() {
        for (BabyLayoutHolder h : holders) {
            h.close();
        }
    }
}

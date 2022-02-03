package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.QuickTimerEntryBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;

public class LoggedInFragment extends BaseFragment {
    private LoggedInFragmentBinding binding;

    private BabyBuddyClient client = null;
    private CredStore credStore = null;

    private BabyPagerAdapter babyAdapter = null;

    private ChildrenStateTracker stateTracker = null;
    private BabyBuddyClient.Child[] children = null;

    private class BabyPagerAdapter extends RecyclerView.Adapter<BabyLayoutHolder> {
        private class HolderChildPair {
            public BabyBuddyClient.Child child;
            public BabyLayoutHolder holder;

            public HolderChildPair(BabyBuddyClient.Child child, BabyLayoutHolder holder) {
                this.child = child;
                this.holder = holder;
            }
        }

        private ArrayList<HolderChildPair> holders = new ArrayList<>();

        @Override
        public BabyLayoutHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            BabyManagerBinding babyBinding = BabyManagerBinding.inflate(
                getLayoutInflater(), null, false
            );
            View v = babyBinding.getRoot();
            v.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT
            ));
            return new BabyLayoutHolder(LoggedInFragment.this, babyBinding);
        }

        @Override
        public void onBindViewHolder(BabyLayoutHolder holder, int position) {
            while (position >= holders.size()) holders.add(null);

            BabyBuddyClient.Child c = children[position];
            holders.set(position, new HolderChildPair(c, holder));
            holder.updateChild(c);
        }

        @Override
        public int getItemCount() {
            if (children == null) {
                return 0;
            }
            return children.length;
        }

        public void activeViewChanged(int position) {
            for (int i = 0; i < holders.size(); i++) {
                BabyLayoutHolder holder = holders.get(position).holder;
                if (holder == null) continue;
                if (i == position) {
                    holder.onViewSelected();
                } else {
                    holder.onViewDeselected();
                }
            }
        }

        public BabyLayoutHolder getHolderFor(BabyBuddyClient.Child c) {
            for (HolderChildPair p : holders) {
                if ((p.child != null) && (p.child.id == c.id)) {
                    return p.holder;
                }
            }
            return null;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        credStore = getMainActivity().getCredStore();
        client = getMainActivity().getClient();

        if (children == null) {
            children = getMainActivity().children;
        }
    }

    @Override
    public View onCreateView(
        LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState
    ) {
        binding = LoggedInFragmentBinding.inflate(inflater, container, false);

        binding.babyViewPagerSwitcher.registerOnPageChangeCallback(
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    babyAdapter.activeViewChanged(position);
                    credStore.setSelectedChild(children == null ? null :children[position].slug);
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
            credStore.storeAppToken(null);
            credStore.clearTimerAssociations();
            Navigation.findNavController(getView()).navigate(R.id.logoutOperation);
        }
        if (item.getItemId() == R.id.aboutPageMenuItem) {
            Navigation.findNavController(getView()).navigate(R.id.action_loggedInFragment2_to_aboutFragment);
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        stateTracker = new ChildrenStateTracker(client, getMainActivity().getMainLooper());
        stateTracker.setChildrenListListener(
            new ChildrenStateTracker.ChildrenListListener() {
                @Override
                public void childrenListUpdated(BabyBuddyClient.Child[] children) {
                    LoggedInFragment.this.children = children;
                    babyAdapter = new BabyPagerAdapter();
                    binding.babyViewPagerSwitcher.setAdapter(babyAdapter);

                    int childIndex = childIndexBySlug(credStore.getSelectedChild());
                    binding.babyViewPagerSwitcher.setCurrentItem(Math.max(0, childIndex), false);

                    updateTitle();
                }
            }
        );
    }

    @Override
    public void onPause() {
        super.onPause();

        stateTracker.close();
        stateTracker = null;
    }

    private int childIndexBySlug(String slug) {
        if (children == null) {
            return -1;
        }
        int i = 0;
        for (BabyBuddyClient.Child c : children) {
            if (Objects.equals(c.slug, slug)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private BabyBuddyClient.Child selectedChild() {
        if (children == null) {
            return null;
        }

        int childIndex = binding.babyViewPagerSwitcher.getCurrentItem();
        BabyBuddyClient.Child child = null;
        int childCount = children != null ? children.length : 0;
        if ((childIndex >= 0) && (childIndex < childCount)) {
            child = children[childIndex];
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

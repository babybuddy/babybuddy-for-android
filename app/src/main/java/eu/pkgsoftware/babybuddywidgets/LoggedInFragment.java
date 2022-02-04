package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
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
        public @NonNull BabyLayoutHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        public void onBindViewHolder(@NonNull BabyLayoutHolder holder, int position) {
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
                if ((p != null) && (p.child != null) && (p.child.id == c.id)) {
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

    private final ChildrenStateTracker.ChildListener CHILD_LISTENER = new ChildrenStateTracker.ChildListener() {
        @Override
        public void childValidUpdated(boolean valid) {

        }

        @Override
        public void timersUpdated(BabyBuddyClient.Timer[] timers) {
            BabyBuddyClient.Child child = selectedChild();
            babyAdapter.getHolderFor(child).updateTimerList(timers);
        }
    };

    @Override
    public View onCreateView(
        @NonNull  LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState
    ) {
        binding = LoggedInFragmentBinding.inflate(inflater, container, false);
        binding.babyViewPagerSwitcher.registerOnPageChangeCallback(
            new ViewPager2.OnPageChangeCallback() {
                private ChildrenStateTracker.ChildObserver childObserver = null;

                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    babyAdapter.activeViewChanged(position);

                    BabyBuddyClient.Child child = children == null ? null : children[position];
                    credStore.setSelectedChild(child == null ? null : child.slug);

                    if (childObserver != null) {
                        childObserver.close();
                        childObserver = null;
                    }
                    if (child != null) {
                        stateTracker.new ChildObserver(child.id, CHILD_LISTENER);
                    }

                    updateTitle();
                }
            }
        );

        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.loggedin_menu, menu);
    }

    private void logout() {
        credStore.storeAppToken(null);
        credStore.clearTimerAssociations();
        Navigation.findNavController(getView()).navigate(R.id.logoutOperation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logoutMenuButton) {
            logout();
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
        stateTracker.new ChildListObserver((children) -> {
            LoggedInFragment.this.children = children;
            babyAdapter = new BabyPagerAdapter();
            binding.babyViewPagerSwitcher.setAdapter(babyAdapter);

            int childIndex = childIndexBySlug(credStore.getSelectedChild());
            binding.babyViewPagerSwitcher.setCurrentItem(Math.max(0, childIndex), false);

            updateTitle();
        });
        stateTracker.setConnectionStateListener(
            (connected, disconnectedFor) -> {
                if (connected) {
                    progressDialog.hide();
                    hideError();
                } else if (disconnectedFor >= 10000) {
                    progressDialog.hide();
                    showQuestion(
                        false,
                        "Connection error",
                        "Server cannot be reached or login failed.",
                        "Keep trying",
                        "Logout",
                        (b) -> {
                            if (b) {
                                showProgress("Connecting to server...");
                                stateTracker.resetDisconnectTimer();
                            } else {
                                logout();
                            }
                        }
                    );
                } else {
                    showProgress("Connecting to server...");
                }
            }
        );
    }

    @Override
    public void onPause() {
        super.onPause();

        progressDialog.hide();

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
        int childCount = children.length;
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

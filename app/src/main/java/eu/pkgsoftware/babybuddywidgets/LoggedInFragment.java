package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
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
        private List<BabyLayoutHolder> holders = new ArrayList<>();
        private BabyLayoutHolder activeHolder = null;

        @Override
        public @NonNull
        BabyLayoutHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            BabyManagerBinding babyBinding = BabyManagerBinding.inflate(
                getLayoutInflater(), null, false
            );
            View v = babyBinding.getRoot();
            v.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));

            BabyLayoutHolder holder = new BabyLayoutHolder(LoggedInFragment.this, babyBinding);
            holders.add(holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull BabyLayoutHolder holder, int position) {
            holder.updateChild(children[position]);

            int childIndex = childIndexBySlug(credStore.getSelectedChild());
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
                    h.onViewSelected(stateTracker);
                    activeHolder = h;
                } else {
                    h.onViewDeselected();
                }
            }
        }

        public void updateChildrenList() {
            notifyDataSetChanged();
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
        @NonNull LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState
    ) {
        binding = LoggedInFragmentBinding.inflate(inflater, container, false);

        babyAdapter = new BabyPagerAdapter();
        binding.babyViewPagerSwitcher.setAdapter(babyAdapter);
        binding.babyViewPagerSwitcher.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                BabyBuddyClient.Child child = children == null ? null : children[position];
                credStore.setSelectedChild(child == null ? null : child.slug);
                babyAdapter.activeViewChanged(child);

                updateTitle();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.loggedin_menu, menu);
    }

    private void logout() {
        credStore.storeAppToken(null);
        credStore.clearTimerAssociations();
        credStore.clearNotes();
        Navigation.findNavController(getView()).navigate(R.id.logoutOperation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logoutMenuButton) {
            logout();
        }
        if (item.getItemId() == R.id.showHelpMenuButton) {
            Navigation.findNavController(getView()).navigate(R.id.global_showHelp);
        }
        if (item.getItemId() == R.id.aboutPageMenuItem) {
            Navigation.findNavController(getView()).navigate(R.id.global_aboutFragment);
        }
        if (item.getItemId() == R.id.recreateTimersButton) {
            if (babyAdapter != null) {
                BabyLayoutHolder holder = babyAdapter.getActive();
                if (holder != null) {
                    holder.recreateDefaultTimers();
                }
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        stateTracker = new ChildrenStateTracker(client, getMainActivity().getMainLooper());
        stateTracker.new ChildListObserver((children) -> {
            LoggedInFragment.this.children = children;
            babyAdapter.updateChildrenList();

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

        credStore.storePrefs();
    }

    @Override
    public void onDestroyView() {
        if (stateTracker != null) {
            stateTracker.close();
            stateTracker = null;
        }
        babyAdapter.close();
        super.onDestroyView();
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

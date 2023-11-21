package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.debugging.GlobalDebugObject;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.ConnectingDialogInterface;
import eu.pkgsoftware.babybuddywidgets.tutorial.Trackable;
import eu.pkgsoftware.babybuddywidgets.tutorial.TutorialEntry;
import eu.pkgsoftware.babybuddywidgets.tutorial.TutorialManagement;

public class LoggedInFragment extends BaseFragment {
    public static int childIndexBySlug(BabyBuddyClient.Child[] children, String slug) {
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

    private LoggedInFragmentBinding binding;

    private BabyBuddyClient client = null;
    private CredStore credStore = null;

    private final EmptyBabyPagerAdapter emptyBabyPagerAdapter = new EmptyBabyPagerAdapter();
    private BabyPagerAdapter babyAdapter = null;

    private ChildrenStateTracker stateTracker = null;
    private BabyBuddyClient.Child[] children = null;

    private ConnectingDialogInterface disconnectInterface = null;

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

        binding.babyViewPagerSwitcher.setAdapter(emptyBabyPagerAdapter);

        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.loggedin_menu, menu);
        menu.findItem(R.id.exportDebugLogsMenuItem).setVisible(GlobalDebugObject.getENABLED());
    }

    private void logout() {
        credStore.clearLoginData();
        Navigation.findNavController(requireView()).navigate(R.id.logoutOperation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logoutMenuButton) {
            logout();
        }
        if (item.getItemId() == R.id.showHelpMenuButton) {
            Navigation.findNavController(requireView()).navigate(R.id.global_showHelp);
        }
        if (item.getItemId() == R.id.aboutPageMenuItem) {
            Navigation.findNavController(requireView()).navigate(R.id.global_aboutFragment);
        }
        if (item.getItemId() == R.id.exportDebugLogsMenuItem) {
            Navigation.findNavController(requireView()).navigate(R.id.action_global_debugLogDisplay);
        }
        return false;
    }

    @Override
    protected void setupTutorialMessages(TutorialManagement m) {
    }

    @Override
    public void onResume() {
        super.onResume();

        disconnectInterface = disconnectDialog.getInterface();

        stateTracker = new ChildrenStateTracker(client, getMainActivity().getMainLooper());
        stateTracker.new ChildListObserver((children) -> {
            if (stateTracker == null) {
                return;
            }

            LoggedInFragment.this.children = children;

            if (babyAdapter == null) {
                babyAdapter = new BabyPagerAdapter();
                babyAdapter.postInit(this, children, stateTracker);
                binding.babyViewPagerSwitcher.setAdapter(babyAdapter);
            } else {
                babyAdapter.updateChildren(children);
            }

            int childIndex = childIndexBySlug(children, credStore.getSelectedChild());
            binding.babyViewPagerSwitcher.setCurrentItem(Math.max(0, childIndex), false);

            this.updateTitle();
        });
        stateTracker.setConnectionStateListener(
            (connected, disconnectedFor) -> {
                if (connected) {
                    disconnectInterface.hideConnecting();
                } else {
                    disconnectInterface.showConnecting(disconnectedFor);
                }
            }
        );

        UpdateNotifications.Companion.showUpdateNotice(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        disconnectInterface.hideConnecting();

        progressDialog.hide();

        stateTracker.close();
        stateTracker = null;

        credStore.storePrefs();

        binding.babyViewPagerSwitcher.setAdapter(emptyBabyPagerAdapter);
        closeAdapter();
    }

    private void closeAdapter() {
        if (babyAdapter != null) {
            babyAdapter.close();
            babyAdapter = null;
        }
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
            getMainActivity().setTitle(getString(R.string.logged_in_no_children_found));
        } else {
            getMainActivity().setTitle(child.first_name + " " + child.last_name);
        }
    }
}

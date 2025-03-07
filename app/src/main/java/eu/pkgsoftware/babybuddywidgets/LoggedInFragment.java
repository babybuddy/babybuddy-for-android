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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.debugging.GlobalDebugObject;
import eu.pkgsoftware.babybuddywidgets.login.LoggedInMenu;
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
    private LoggedInMenu menu;

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
                BabyBuddyClient.Child child = null;
                if (children != null) {
                    if (position >= children.length) {
                        position = children.length - 1;
                    }
                    if (position < 0) {
                        position = 0;
                    }
                    if (children.length > 0) {
                        child = children[position];
                    }
                }

                super.onPageSelected(position);

                credStore.setSelectedChild(child == null ? null : child.slug);
                babyAdapter.activeViewChanged(child);

                updateTitle();
            }
        });

        binding.babyViewPagerSwitcher.setAdapter(emptyBabyPagerAdapter);

        return binding.getRoot();
    }

    @Override
    protected void setupTutorialMessages(@NonNull TutorialManagement m) {
    }

    @Override
    public void onResume() {
        super.onResume();

        if (menu == null) {
            menu = new LoggedInMenu(this);
        }
        getMainActivity().addMenuProvider(menu);

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
                    disconnectInterface.showConnecting(disconnectedFor, null);
                }
            }
        );

        UpdateNotifications.Companion.showUpdateNotice(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getMainActivity().removeMenuProvider(menu);
        getMainActivity().invalidateOptionsMenu();

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

package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding;
import eu.pkgsoftware.babybuddywidgets.logic.ChildrenStateTracker;
import eu.pkgsoftware.babybuddywidgets.login.LoggedInMenu;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.ConnectingDialogInterface;
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Child;
import eu.pkgsoftware.babybuddywidgets.tutorial.TutorialManagement;
import kotlin.Unit;

public class LoggedInFragment extends BaseFragment {
    private LoggedInFragmentBinding binding;
    private LoggedInMenu menu;

    private BabyBuddyClient client = null;
    private CredStore credStore = null;

    private final EmptyBabyPagerAdapter emptyBabyPagerAdapter = new EmptyBabyPagerAdapter();
    private BabyPagerAdapter babyAdapter = null;

    private ChildrenStateTracker stateTracker = null;
    private final List<Child> children = new ArrayList<>();

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

        stateTracker = new ChildrenStateTracker(client.v2client, getMainActivity().getStorage());
        stateTracker.addChildListener(children -> {
            this.children.clear();
            Collections.addAll(this.children, children);

            String childSlug = credStore.getSelectedChild();
            if ((childSlug == null) && (!this.children.isEmpty())) {
                childSlug = this.children.get(0).getSlug();
                credStore.setSelectedChild(childSlug);
                binding.babyViewPagerSwitcher.setCurrentItem(0, false);
            }

            return Unit.INSTANCE;
        }, true);
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
                Child child = null;
                if (children != null) {
                    if (position >= children.size()) {
                        position = children.size() - 1;
                    }
                    if (position < 0) {
                        position = 0;
                    }
                    if (!children.isEmpty()) {
                        child = children.get(position);
                    }
                }

                super.onPageSelected(position);

                credStore.setSelectedChild(child == null ? null : child.getSlug());
                if (child != null) {
                    babyAdapter.activeViewChanged(child);
                }

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

        stateTracker.startPolling();

        if (menu == null) {
            menu = new LoggedInMenu(this);
        }
        getMainActivity().addMenuProvider(menu);

        disconnectInterface = disconnectDialog.getInterface();

        if (babyAdapter == null) {
            babyAdapter = new BabyPagerAdapter(stateTracker);
            babyAdapter.postInit(this);
            binding.babyViewPagerSwitcher.setAdapter(babyAdapter);
        }


        String childSlug = credStore.getSelectedChild();
        if ((childSlug == null) && (!children.isEmpty())) {
            childSlug = children.get(0).getSlug();
        }
        if (childSlug != null) {
            int childIndex = Child.Companion.childIndexBySlug(
                children.toArray(new Child[0]), childSlug);
            binding.babyViewPagerSwitcher.setCurrentItem(Math.max(0, childIndex), false);
        }

        this.updateTitle();

        /*stateTracker.setConnectionStateListener(
            (connected, disconnectedFor) -> {
                if (connected) {
                    disconnectInterface.hideConnecting();
                } else {
                    disconnectInterface.showConnecting(disconnectedFor, null);
                }
            }
        );*/

        UpdateNotifications.Companion.showUpdateNotice(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        stateTracker.stopPolling();

        getMainActivity().removeMenuProvider(menu);
        getMainActivity().invalidateOptionsMenu();

        disconnectInterface.hideConnecting();

        progressDialog.hide();
        credStore.storePrefs();

        binding.babyViewPagerSwitcher.setAdapter(emptyBabyPagerAdapter);
        closeAdapter();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stateTracker = null;
        client = null;
        credStore = null;
    }

    private void closeAdapter() {
        if (babyAdapter != null) {
            babyAdapter.close();
            babyAdapter = null;
        }
    }

    private Child selectedChild() {
        if (children == null) {
            return null;
        }

        int childIndex = binding.babyViewPagerSwitcher.getCurrentItem();
        Child child = null;
        int childCount = children.size();
        if ((childIndex >= 0) && (childIndex < childCount)) {
            child = children.get(childIndex);
        }
        return child;
    }

    private void updateTitle() {
        Child child = selectedChild();
        if (child == null) {
            getMainActivity().setTitle(getString(R.string.logged_in_no_children_found));
        } else {
            getMainActivity().setTitle(child.getFirstName() + " " + child.getLastName());
        }
    }
}

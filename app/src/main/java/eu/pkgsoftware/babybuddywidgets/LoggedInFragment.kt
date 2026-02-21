package eu.pkgsoftware.babybuddywidgets

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import eu.pkgsoftware.babybuddywidgets.UpdateNotifications.Companion.showUpdateNotice
import eu.pkgsoftware.babybuddywidgets.databinding.LoggedInFragmentBinding
import eu.pkgsoftware.babybuddywidgets.logic.ApplicationInterface
import eu.pkgsoftware.babybuddywidgets.logic.ChildrenStateTracker
import eu.pkgsoftware.babybuddywidgets.logic.RequestScheduler
import eu.pkgsoftware.babybuddywidgets.login.LoggedInMenu
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.ConnectingDialogInterface
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Child
import eu.pkgsoftware.babybuddywidgets.tutorial.TutorialManagement
import java.util.Collections
import kotlin.math.max

class LoggedInFragment : BaseFragment() {
    private lateinit var binding: LoggedInFragmentBinding
    private lateinit var menu: LoggedInMenu

    private val emptyBabyPagerAdapter = EmptyBabyPagerAdapter()
    private var babyAdapter: BabyPagerAdapter? = null

    private lateinit var requestScheduler: RequestScheduler
    private var stateTracker: ChildrenStateTracker? = null

    private var selectedChildSlug = ""
    private var children = listOf<Child>()

    private var disconnectInterface: ConnectingDialogInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
        selectedChildSlug = mainActivity.storage.login<String>("selected-child-slug") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        menu = LoggedInMenu(this)

        binding = LoggedInFragmentBinding.inflate(inflater, container, false)
        binding.babyViewPagerSwitcher.registerOnPageChangeCallback(object :
            OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                var position = position
                if (position >= children.size) {
                    position = children.size - 1
                }
                if (position < 0) {
                    position = 0
                }
                if (!children.isEmpty()) {
                    selectChild(children.get(position))
                }

                super.onPageSelected(position)
            }
        })

        binding.babyViewPagerSwitcher.setAdapter(emptyBabyPagerAdapter)

        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        disconnectInterface = disconnectDialog.getInterface()
        requestScheduler = RequestScheduler(object : ApplicationInterface {
            private var isConnected = true
            private var lastDisconnectTime: Long = 0
            private val loopHandler = Handler(mainActivity.mainLooper)

            override fun setDisconnected(reason: String, disconnected: Boolean) {
                if (disconnectInterface == null) {
                    return
                }
                isConnected = !disconnected
                if (disconnected) {
                    lastDisconnectTime = System.currentTimeMillis()
                    loopHandler.post(Runnable { this.updateDisconnecting() })
                } else {
                    disconnectInterface!!.hideConnecting()
                }
            }

            fun updateDisconnecting() {
                if (disconnectInterface == null) {
                    return
                }
                if (isConnected) {
                    return
                }
                val disconnectedFor = System.currentTimeMillis() - lastDisconnectTime
                disconnectInterface!!.showConnecting(disconnectedFor, null)
                loopHandler.postDelayed(Runnable { this.updateDisconnecting() }, 100)
            }

            override fun reportError(message: String, error: Exception?) {
                if (error != null) {
                    error.printStackTrace()
                } else {
                    System.err.println("RequestScheduler Error: $message")
                }
            }
        })

        stateTracker = ChildrenStateTracker(
            mainActivity.client.v2client, mainActivity.storage, requestScheduler
        )
        stateTracker!!.addChildListener({ children: Array<Child> ->
            this.children = Collections.unmodifiableList(children.toList())

            if (!this.children.isEmpty()) {
                for (c in this.children) {
                    if (c.slug == selectedChildSlug) {
                        selectChild(c)
                    }
                }
            }
        }, true)
    }

    override fun setupTutorialMessages(m: TutorialManagement) {
    }

    override fun onResume() {
        super.onResume()

        requestScheduler.startScheduler()

        mainActivity.addMenuProvider(menu)

        if (babyAdapter == null) {
            babyAdapter = run {
                val bpa = BabyPagerAdapter(stateTracker!!)
                bpa.postInit(this)
                binding.babyViewPagerSwitcher.setAdapter(bpa)
                bpa
            }
        }


        val childSlug = selectedChildSlug
        val childIndex = Child.childIndexBySlug(children.toTypedArray(), childSlug)
        val clampedIndex = max(0, childIndex)
        binding.babyViewPagerSwitcher.setCurrentItem(clampedIndex, false)
        if (childIndex >= 0) {
            babyAdapter?.activeViewChanged(children.get(clampedIndex))
        }

        this.updateTitle()

        showUpdateNotice(this)
    }

    override fun onPause() {
        super.onPause()

        requestScheduler.stopScheduler()

        mainActivity.removeMenuProvider(menu)
        mainActivity.invalidateOptionsMenu()

        disconnectInterface!!.hideConnecting()

        progressDialog.hide()
        mainActivity.credStore.storePrefs()

        binding.babyViewPagerSwitcher.setAdapter(emptyBabyPagerAdapter)
        closeAdapter()
    }

    override fun onDetach() {
        super.onDetach()
        stateTracker = null
    }

    private fun closeAdapter() {
        if (babyAdapter != null) {
            babyAdapter!!.close()
            babyAdapter = null
        }
    }

    private fun selectedChild(): Child? {
        val childIndex = binding.babyViewPagerSwitcher.getCurrentItem()
        var child: Child? = null
        val childCount = children.size
        if ((childIndex >= 0) && (childIndex < childCount)) {
            child = children.get(childIndex)
        }
        return child
    }

    private fun selectChild(c: Child) {
        val childIndex = Child.childIndexBySlug(children.toTypedArray(), c.slug)
        mainActivity.storage.login<String>("selected-child-slug", c.slug)
        selectedChildSlug = c.slug

        babyAdapter?.activeViewChanged(c)
        if (childIndex != binding.babyViewPagerSwitcher.getCurrentItem()) {
            binding.babyViewPagerSwitcher.setCurrentItem(max(0, childIndex), true)
        }

        updateTitle()
    }

    private fun updateTitle() {
        val child = selectedChild()
        if (child == null) {
            mainActivity.setTitle(getString(R.string.logged_in_no_children_found))
        } else {
            mainActivity.setTitle(child.firstName + " " + child.lastName)
        }
    }
}

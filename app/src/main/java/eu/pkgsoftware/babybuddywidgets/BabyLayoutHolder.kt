package eu.pkgsoftware.babybuddywidgets

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.squareup.phrase.Phrase
import eu.pkgsoftware.babybuddywidgets.activitycomponents.TimerControl
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding
import eu.pkgsoftware.babybuddywidgets.history.ChildEventHistoryLoader
import eu.pkgsoftware.babybuddywidgets.history.ShowErrorPill
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Child
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import eu.pkgsoftware.babybuddywidgets.timers.FragmentCallbacks
import eu.pkgsoftware.babybuddywidgets.timers.LoggingButtonController
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback
import eu.pkgsoftware.babybuddywidgets.timers.TranslatedException
import eu.pkgsoftware.babybuddywidgets.utils.Promise

class BabyLayoutHolder(
    private val baseFragment: BaseFragment,
    private val binding: BabyManagerBinding
) : RecyclerView.ViewHolder(
    binding.getRoot()
), TimerControlInterface {
    private val client: BabyBuddyClient

    var child: Child? = null

    private var childHistoryLoader: ChildEventHistoryLoader? = null

    private var cachedTimers: Array<BabyBuddyClient.Timer>? = null
    private val updateTimersCallbacks: MutableList<TimersUpdatedCallback> =
        ArrayList(10)

    private var pendingTimerModificationCalls = 0

    private var loggingButtonController: LoggingButtonController? = null

    init {
        client = baseFragment.mainActivity.client

        binding.mainScrollView.setOnScrollChangeListener(View.OnScrollChangeListener { v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            if (childHistoryLoader != null) {
                childHistoryLoader!!.updateTop()
            }
        })
    }

    private fun resetChildHistoryLoader() {
        if (childHistoryLoader != null) {
            childHistoryLoader!!.close()
        }
        childHistoryLoader = null
    }

    fun updateChild(c: Child) {
        if (child === c) {
            return
        }

        clear()
        this.child = c

        child ?.let { child ->
            childHistoryLoader = ChildEventHistoryLoader(
                baseFragment,
                binding.innerTimeline,
                child.id,
                VisibilityCheck(binding.mainScrollView),
                binding.timelineProgressSpinner,
                object : ShowErrorPill {
                    override fun showErrorPill(entryType: String, exception: Exception?) {
                        val tActivity = baseFragment.translateActivityName(entryType!!)
                        val msg = Phrase.from(
                            baseFragment.getResources(),
                            R.string.history_loading_timeline_entry_failed
                        )
                            .put("activity", tActivity)
                            .format().toString()
                        baseFragment.mainActivity.binding.globalErrorBubble.flashMessage(msg)
                    }
                }
            )

            loggingButtonController = LoggingButtonController(
                baseFragment,
                binding,
                object : FragmentCallbacks {
                    override fun insertControls(view: View) {
                        if (view.getParent() != null) {
                            return
                        }
                        binding.loggingEditors.addView(view)
                    }

                    override fun removeControls(view: View) {
                        if (view.getParent() == null) {
                            return
                        }
                        binding.loggingEditors.removeView(view)
                    }

                    override fun updateTimeline(newEntry: TimeEntry?) {
                        if (childHistoryLoader != null) {
                            if (newEntry != null) {
                                childHistoryLoader!!.addEntryToTop(newEntry)
                            }
                            childHistoryLoader!!.forceRefresh()
                        }
                    }
                },
                child,
                this
            )
        }
    }

    fun updateTimerList(timers: Array<BabyBuddyClient.Timer>) {
        if (pendingTimerModificationCalls > 0) {
            // Buffer timer-list updates while a timer-modifying operation is running
            // (prevents confusing UI updates)
            return
        }

        if (child == null) {
            cachedTimers = arrayOf()
            callTimerUpdateCallback()
            return
        }

        for (t in timers) {
            child?.let { child ->
                if (t.child_id != child.id) {
                    return
                }
            } ?: return
        }

        cachedTimers = timers
        callTimerUpdateCallback()
    }

    fun onViewDeselected() {
        if (loggingButtonController != null) {
            loggingButtonController!!.storeStateForSuspend()
        }
        resetChildHistoryLoader()
    }

    fun clear() {
        if (loggingButtonController != null) {
            loggingButtonController!!.storeStateForSuspend()
            loggingButtonController!!.destroy()
            loggingButtonController = null
        }
        resetChildHistoryLoader()
        child = null
        cachedTimers = null
    }

    fun close() {
        clear()
    }

    private inner class UpdateBufferingPromise<A, B>(private val promise: Promise<A, B>) :
        Promise<A, B> {
        init {
            pendingTimerModificationCalls++
        }

        override fun succeeded(a: A) {
            pendingTimerModificationCalls--
            promise.succeeded(a)
        }

        override fun failed(b: B) {
            pendingTimerModificationCalls--
            promise.failed(b)
        }
    }

    override fun createNewTimer(
        timer: BabyBuddyClient.Timer,
        cb: Promise<BabyBuddyClient.Timer, TranslatedException>
    ) {
        child?.let { child ->
            baseFragment.mainActivity.getChildTimerControl(child.id).createNewTimer(
                timer, UpdateBufferingPromise(cb)
            )
        }
    }

    override fun startTimer(
        timer: BabyBuddyClient.Timer,
        cb: Promise<BabyBuddyClient.Timer, TranslatedException>
    ) {
        child?.let { child ->
            baseFragment.mainActivity.getChildTimerControl(child.id).startTimer(
                timer, UpdateBufferingPromise(cb)
            )
        }
    }

    override fun stopTimer(timer: BabyBuddyClient.Timer, cb: Promise<Any, TranslatedException>) {
        child?.let { child ->
            baseFragment.mainActivity.getChildTimerControl(child.id).stopTimer(
                timer, UpdateBufferingPromise(cb)
            )
        }
    }

    override fun registerTimersUpdatedCallback(callback: TimersUpdatedCallback) {
        child?.let { child ->
            if (updateTimersCallbacks.contains(callback)) {
                return
            }
            updateTimersCallbacks.add(callback)

            baseFragment.mainActivity.getChildTimerControl(child.id).registerTimersUpdatedCallback(
                object : TimersUpdatedCallback {
                    override fun newTimerListLoaded(timers: Array<BabyBuddyClient.Timer>) {
                        for (c in updateTimersCallbacks) {
                            c.newTimerListLoaded(timers)
                        }
                    }
                }
            )

            callTimerUpdateCallback()
        }
    }

    override fun unregisterTimersUpdatedCallback(callback: TimersUpdatedCallback) {
        if (!updateTimersCallbacks.contains(callback)) {
            return
        }
        updateTimersCallbacks.remove(callback)
    }

    private fun callTimerUpdateCallback() {
        // urgh...
        child?.let { child ->
            val wrapped = baseFragment.mainActivity.getChildTimerControl(child.id).wrap as TimerControl
            if (cachedTimers != null) {
                wrapped.callTimerUpdateCallback(cachedTimers!!)
            }
        }
    }

    override fun getNotes(timer: BabyBuddyClient.Timer): CredStore.Notes {
        return child?.let { child ->
            return@let baseFragment.mainActivity.getChildTimerControl(child.id).getNotes(timer)
        } ?: CredStore.Notes("", false)
    }

    override fun setNotes(timer: BabyBuddyClient.Timer, notes: CredStore.Notes?) {
        child?.let { child ->
            baseFragment.mainActivity.getChildTimerControl(child.id).setNotes(timer, notes)
        }
    }
}

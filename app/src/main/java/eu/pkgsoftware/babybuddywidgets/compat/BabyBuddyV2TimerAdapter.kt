package eu.pkgsoftware.babybuddywidgets.compat

import android.content.Context
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback
import eu.pkgsoftware.babybuddywidgets.utils.Promise

class BabyBuddyV2TimerAdapter(
    val child: Child,
    val context: Context,
    val wrap: TimerControlInterface) : TimerControlInterface
{
    private val virtualTimers: Array<Timer>
    private val actualTimers = mutableListOf<Timer>()
    private var timersCallback: TimersUpdatedCallback? = null

    init {
        val activityNames = context.resources.getStringArray(R.array.timerTypeNames)

        virtualTimers = (0 until ACTIVITIES.ALL.size).map {
            val t = Timer()
            t.id = it;
            t.user_id = 0;
            t.child_id = child.id;
            t.active = false;
            t.name = activityNames[it];
            t.start = null;
            t.end = null;
            t
        }.toTypedArray()

        wrap.registerTimersUpdatedCallback(object : TimersUpdatedCallback {
            override fun newTimerListLoaded(timers: Array<Timer>) {
                actualTimers.clear()
                actualTimers.addAll(timers)
            }
        })
    }

    private fun triggerTimerCallback() {
        timersCallback?.let {
            it.newTimerListLoaded(virtualTimers)
        }

    }

    override fun startTimer(timer: Timer, cb: Promise<Timer, String>) {
    }

    override fun stopTimer(timer: Timer, cb: Promise<Any, String>) {
    }

    override fun storeActivity(
        timer: Timer,
        activity: String,
        notes: String,
        cb: Promise<Boolean, Exception>
    ) {
    }

    override fun registerTimersUpdatedCallback(callback: TimersUpdatedCallback) {
        timersCallback = callback
        triggerTimerCallback()
    }
}
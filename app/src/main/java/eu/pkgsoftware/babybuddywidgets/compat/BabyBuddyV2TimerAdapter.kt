package eu.pkgsoftware.babybuddywidgets.compat

import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import java.util.Locale
import kotlin.concurrent.timer

class BabyBuddyV2TimerAdapter(
    val child: Child,
    val wrap: TimerControlInterface
) : TimerControlInterface {
    private val virtualTimers: Array<Timer>
    private var timersCallback: TimersUpdatedCallback? = null
    private var actualTimers: List<Timer>? = null

    companion object {
        val STRUCTURED_REGEX = "^.*-bbapp:([0-9]+)$".toRegex()
        val OLD_READABLE_NAMES = mapOf(
            "sleep" to ACTIVITIES.SLEEP,
            "tummy time" to ACTIVITIES.TUMMY_TIME,
            "feeding" to ACTIVITIES.FEEDING,
        )

        fun mapBabyBuddyNameToActivity(name: String): String? {
            val cleanName = name.trim().lowercase(Locale.getDefault())

            STRUCTURED_REGEX.matchEntire(cleanName)?.let {
                val i = it.groupValues[1].toInt()
                if ((i <= 0) || (i > ACTIVITIES.ALL.size)) {
                    return null
                }
                return ACTIVITIES.ALL[i - 1]
            }

            if (OLD_READABLE_NAMES.containsKey(cleanName)) {
                return OLD_READABLE_NAMES[cleanName]
            }

            for (ref in ACTIVITIES.ALL) {
                if (ref == cleanName) {
                    return ref
                }
            }

            return null
        }
    }

    init {
        virtualTimers = (0 until ACTIVITIES.ALL.size).map {
            val t = Timer()
            t.id = it
            t.user_id = 0
            t.child_id = child.id
            t.active = false
            t.name = ACTIVITIES.ALL[it]
            t.start = null
            t.end = null
            t
        }.toTypedArray()

        wrap.registerTimersUpdatedCallback(object : TimersUpdatedCallback {
            override fun newTimerListLoaded(timers: Array<Timer>) {
                actualTimers = timers.toList()
                triggerTimerCallback()
            }
        })
    }

    private fun triggerTimerCallback() {
        timersCallback?.let { callback ->
            actualTimers?.let { actualTimers ->
                val timerList = virtualTimers.toMutableList()

                for (actTimer in actualTimers) {
                    val mappedActName = mapBabyBuddyNameToActivity(actTimer.name) ?: continue
                    val actI = ACTIVITIES.index(mappedActName)

                    val virtTimer = virtualTimers[actI]
                    val newTimer = actTimer.clone()
                    newTimer.name = virtTimer.name
                    newTimer.id = virtTimer.id
                    timerList[actI] = newTimer
                }

                callback.newTimerListLoaded(timerList.toTypedArray())
            } ?: {
                callback.newTimerListLoaded(arrayOf())
            }
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
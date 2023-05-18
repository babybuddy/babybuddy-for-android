package eu.pkgsoftware.babybuddywidgets.compat

import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import java.util.Locale

data class WrappedTimer(val mappedActivityIndex: Int, val timer: Timer) {
}

class BabyBuddyV2TimerAdapter(
    val child: Child,
    val wrap: TimerControlInterface
) : TimerControlInterface {
    private val virtualTimers: Array<Timer>
    private var timersCallback: TimersUpdatedCallback? = null
    private var actualTimers: List<WrappedTimer>? = null

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

        fun mapBabyBuddyNameToActivityIndex(name: String): Int {
            mapBabyBuddyNameToActivity(name)?.let {
                return ACTIVITIES.index(it)
            }
            return -1
        }
    }

    init {
        virtualTimers = (0 until ACTIVITIES.ALL.size).map {
            val t = Timer()
            t.id = it + ACTIVITIES.ALL.size * child.id
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
                actualTimers = timers.map {
                    WrappedTimer(mapBabyBuddyNameToActivityIndex(it.readableName()), it)
                }
                triggerTimerCallback()
            }
        })
    }

    private fun triggerTimerCallback() {
        timersCallback?.let { callback ->
            actualTimers?.let { actualTimers ->
                val timerList = virtualTimers.toMutableList()

                for (actTimer in actualTimers) {
                    if (actTimer.mappedActivityIndex < 0) {
                        continue
                    }
                    val newTimer = timerToVirtualTimer(actTimer.timer) ?: continue
                    timerList[ACTIVITIES.index(newTimer.name)] = newTimer
                }

                callback.newTimerListLoaded(timerList.toTypedArray())
            } ?: {
                callback.newTimerListLoaded(arrayOf())
            }
        }
    }

    private fun virtualToActualTimer(timer: Timer): Timer? {
        val activityIndex = ACTIVITIES.index(timer.name)
        if (activityIndex < 0) {
            return null
        }
        actualTimers?.let { actualTimers ->
            for (actTimer in actualTimers) {
                if (actTimer.mappedActivityIndex == activityIndex) {
                    return actTimer.timer
                }
            }
        }
        return null
    }

    private fun timerToVirtualTimer(timer: Timer): Timer? {
        val mappedActName = mapBabyBuddyNameToActivity(timer.name) ?: return null
        val activityIndex = ACTIVITIES.index(mappedActName)

        val virtTimer = virtualTimers[activityIndex]
        val newTimer = timer.clone()
        newTimer.name = virtTimer.name
        newTimer.id = virtTimer.id
        return newTimer
    }

    override fun startTimer(timer: Timer, cb: Promise<Timer, String>) {
        virtualToActualTimer(timer)?.let {
            cb.failed("Timer for activity ${timer.name} already exists")
            return
        }

        val newTimer = timer.clone()
        val actI = ACTIVITIES.index(newTimer.name)
        if (actI < 0) {
            cb.failed("Invalid activity ${newTimer.name}")
        } else {
            newTimer.name = "${newTimer.name}-BBapp:${actI + 1}"
            wrap.startTimer(newTimer, object : Promise<Timer, String> {
                override fun succeeded(s: Timer?) {
                    if (s == null) {
                        cb.succeeded(null)
                    } else {
                        val vTimer = timerToVirtualTimer(timer)
                        cb.succeeded(vTimer)
                    }
                }

                override fun failed(f: String?) {
                    cb.failed(f)
                }
            })
        }
    }

    override fun stopTimer(timer: Timer, cb: Promise<Any, String>) {
        virtualToActualTimer(timer)?.let {
            wrap.stopTimer(it, cb)
        } ?: {
            cb.failed("Timer ${timer.name} does not exist")
        }
    }

    override fun storeActivity(
        timer: Timer,
        activity: String,
        notes: String,
        cb: Promise<Boolean, Exception>
    ) {
        virtualToActualTimer(timer)?.let {
            wrap.storeActivity(it, activity, notes, cb)
        } ?: {
            cb.failed(java.lang.Exception("Timer ${timer.name} does not exist"))
        }
    }

    override fun registerTimersUpdatedCallback(callback: TimersUpdatedCallback) {
        timersCallback = callback
        triggerTimerCallback()
    }
}
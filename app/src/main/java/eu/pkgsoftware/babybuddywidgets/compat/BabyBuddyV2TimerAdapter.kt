package eu.pkgsoftware.babybuddywidgets.compat

import android.content.res.Resources
import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.history.IMPLEMENTED_EVENT_CLASSES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.classActivityName
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback
import eu.pkgsoftware.babybuddywidgets.timers.TranslatedException
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import java.util.Locale

data class WrappedTimer(val mappedActivityIndex: Int, val timer: Timer) {
}

val IMPLEMENTED_ACTIVITIES = IMPLEMENTED_EVENT_CLASSES.map {classActivityName(it) }.filter { it in ACTIVITIES.ALL }.toList()

class BabyBuddyV2TimerAdapter(
    val childId: Int,
    val wrap: TimerControlInterface,
    val resources: Resources,
    val credStore: CredStore
) : TimerControlInterface {
    private val virtualTimers: Array<Timer>
    private var timersCallback: TimersUpdatedCallback? = null
    private var actualTimers: MutableList<WrappedTimer>? = null

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
        virtualTimers = (0 until IMPLEMENTED_ACTIVITIES.size).map {
            val t = Timer()
            t.id = it + ACTIVITIES.ALL.size * childId
            t.user_id = 0
            t.child_id = childId
            t.active = false
            t.name = IMPLEMENTED_ACTIVITIES[it]
            t.start = null
            t.end = null
            t
        }.toTypedArray()

        wrap.registerTimersUpdatedCallback(object : TimersUpdatedCallback {
            override fun newTimerListLoaded(timers: Array<Timer>) {
                actualTimers = timers.map {
                    WrappedTimer(mapBabyBuddyNameToActivityIndex(it.readableName()), it)
                }.toMutableList()
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
                    timerList[IMPLEMENTED_ACTIVITIES.indexOf(newTimer.name)] = newTimer
                }

                callback.newTimerListLoaded(timerList.toTypedArray())
            } ?: {
                callback.newTimerListLoaded(arrayOf())
            }
        }
    }

    fun virtualToActualTimer(timer: Timer): Timer? {
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

    fun timerToVirtualTimer(timer: Timer): Timer? {
        val mappedActName = mapBabyBuddyNameToActivity(timer.name) ?: return null
        val virtActivityIndex = IMPLEMENTED_ACTIVITIES.indexOf(mappedActName)

        val virtTimer = virtualTimers[virtActivityIndex]
        val newTimer = timer.clone()
        newTimer.name = virtTimer.name
        newTimer.id = virtTimer.id
        return newTimer
    }

    override fun createNewTimer(timer: Timer, cb: Promise<Timer, TranslatedException>) {
        startTimer(timer, cb)
    }

    override fun startTimer(timer: Timer, cb: Promise<Timer, TranslatedException>) {
        var existingTimer = false
        val timerToStart: Timer = virtualToActualTimer(timer)?.let {
            if (it.active) {
                cb.failed(
                    TranslatedException("Timer for activity ${timer.name} already active", null)
                )
                return
            }
            existingTimer = true
            it
        } ?: run {
            val actI = ACTIVITIES.index(timer.name)
            if (actI < 0) {
                cb.failed(TranslatedException("Invalid activity ${timer.name}", null))
                return
            }

            val t = timer.clone()
            val readableActivityName = resources.getStringArray(R.array.timerTypeNames)
            t.name = "${readableActivityName[actI]}-BBapp:${actI + 1}"
            t
        }

        fun success(s: Timer?) {
            if (s == null) {
                cb.succeeded(null)
            } else {
                val vTimer = timerToVirtualTimer(s)
                cb.succeeded(vTimer)
                if (vTimer != null) {
                    actualTimers?.let {
                        val actI = mapBabyBuddyNameToActivityIndex(vTimer.name)
                        var updated = false
                        val newTimer = WrappedTimer(actI, s)
                        for (i in 0 until it.size) {
                            if (it[i].mappedActivityIndex == actI) {
                                it[i] = newTimer
                                updated = true
                            }
                        }
                        if (!updated) {
                            it.add(newTimer)
                        }
                    }
                    wrap.setNotes(s, credStore.getObjectNotes("virttimer_${childId}_${vTimer.id}"))
                }
            }
        }

        fun doV2CreateRequest() {
            wrap.createNewTimer(timerToStart, object : Promise<Timer, TranslatedException> {
                override fun succeeded(s: Timer?) {
                    success(s)
                }

                override fun failed(f: TranslatedException?) {
                    cb.failed(f)
                }
            });
        }

        if (!existingTimer) {
            doV2CreateRequest()
            return
        } else {
            // For version 1.x compatibility, we try to restart the pre-existing timer first
            wrap.startTimer(timerToStart, object : Promise<Timer, TranslatedException> {
                override fun succeeded(s: Timer?) {
                    success(s)
                }

                override fun failed(f: TranslatedException?) {
                    f?.originalError?.let {
                        if (it is RequestCodeFailure) {
                            if (it.code == 404) {
                                // This is likely a version 2.0 request that is required
                                doV2CreateRequest()
                                return
                            }
                        }
                    }
                    cb.failed(f)
                }
            })
        }
    }

    override fun stopTimer(timer: Timer, cb: Promise<Any, TranslatedException>) {
        virtualToActualTimer(timer)?.let {
            wrap.stopTimer(it, cb)
        } ?: {
            cb.failed(TranslatedException("Timer ${timer.name} does not exist", null))
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

    override fun getNotes(timer: Timer): CredStore.Notes {
        virtualToActualTimer(timer)?.let {
            return wrap.getNotes(it)
        }
        return credStore.getObjectNotes("virttimer_${childId}_${timer.id}")
    }

    override fun setNotes(timer: Timer, notes: CredStore.Notes?) {
        val n = notes ?: CredStore.EMPTY_NOTES
        credStore.setObjectNotes("virttimer_${childId}_${timer.id}", n.visible, n.note)
        virtualToActualTimer(timer)?.let {
            wrap.setNotes(it, notes)
        }
    }
}

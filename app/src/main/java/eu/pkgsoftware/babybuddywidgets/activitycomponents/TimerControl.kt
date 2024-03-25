package eu.pkgsoftware.babybuddywidgets.activitycomponents

import eu.pkgsoftware.babybuddywidgets.CredStore
import eu.pkgsoftware.babybuddywidgets.MainActivity
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.RequestCallback
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer
import eu.pkgsoftware.babybuddywidgets.timers.StoreActivityRouter
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback
import eu.pkgsoftware.babybuddywidgets.timers.TranslatedException
import eu.pkgsoftware.babybuddywidgets.utils.Promise

class TimerControl(val mainActivity: MainActivity, val childId: Int) : TimerControlInterface {
    private val storeActivityRouter = StoreActivityRouter(mainActivity)
    private val client = mainActivity.client

    var updateTimersCallback: TimersUpdatedCallback? = null

    fun callTimerUpdateCallback(timers: Array<Timer>) {
        updateTimersCallback?.newTimerListLoaded(timers)
    }

    override fun createNewTimer(timer: Timer, cb: Promise<Timer, TranslatedException>) {
        client.createTimer(childId, timer.name, object : RequestCallback<Timer> {
            override fun error(error: Exception) {
                cb.failed(
                    TranslatedException(
                        mainActivity.getString(R.string.activity_store_failure_start_timer_failed),
                        error
                    )
                )
            }

            override fun response(response: Timer) {
                cb.succeeded(response)
            }
        })
    }

    override fun startTimer(timer: Timer, cb: Promise<Timer, TranslatedException>) {
        client.restartTimer(timer.id, object : RequestCallback<Timer> {
            override fun error(error: Exception) {
                cb.failed(
                    TranslatedException(
                        mainActivity.getString(R.string.activity_store_failure_start_timer_failed),
                        error
                    )
                )
            }

            override fun response(timer: Timer) {
                cb.succeeded(timer)
            }
        })
    }

    override fun stopTimer(timer: Timer, cb: Promise<Any, TranslatedException>) {
        client.deleteTimer(timer.id, object : RequestCallback<Boolean> {
            override fun error(error: Exception) {
                cb.failed(
                    TranslatedException(
                        mainActivity.getString(R.string.activity_store_failure_failed_to_stop_message),
                        error
                    )
                )
            }

            override fun response(response: Boolean) {
                cb.succeeded(Any())
            }
        })
    }

    override fun storeActivity(
        timer: Timer,
        activity: String,
        notes: String,
        cb: Promise<Boolean, Exception>
    ) {
        storeActivityRouter.store(activity, notes, timer, object : Promise<Boolean, Exception> {
            override fun succeeded(aBoolean: Boolean) {
                cb.succeeded(aBoolean)
            }

            override fun failed(e: Exception) {
                cb.failed(e)
            }
        })
    }

    override fun registerTimersUpdatedCallback(callback: TimersUpdatedCallback) {
        updateTimersCallback = callback
    }

    override fun unregisterTimersUpdatedCallback(callback: TimersUpdatedCallback) {
        if (updateTimersCallback == callback) {
            updateTimersCallback = null
        }
    }

    override fun getNotes(timer: Timer): CredStore.Notes {
        val credStore: CredStore = mainActivity.credStore
        return credStore.getObjectNotes("timer_" + timer.id)
    }

    override fun setNotes(timer: Timer, notes: CredStore.Notes?) {
        val credStore: CredStore = mainActivity.credStore
        val key = "timer_" + timer.id
        if (notes == null) {
            credStore.setObjectNotes(key, false, "")
        } else {
            credStore.setObjectNotes(key, notes.visible, notes.note)
        }
    }
}

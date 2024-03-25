package eu.pkgsoftware.babybuddywidgets.timers

import eu.pkgsoftware.babybuddywidgets.CredStore.Notes
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer

interface TimersUpdatedCallback {
    fun newTimerListLoaded(timers: Array<Timer>)
}

class TranslatedException(message: String, val originalError: java.lang.Exception?) : Exception(message) {
}

interface TimerControlInterface {
    fun createNewTimer(timer: Timer, cb: Promise<Timer, TranslatedException>)
    fun startTimer(timer: Timer, cb: Promise<Timer, TranslatedException>)
    fun stopTimer(timer: Timer, cb: Promise<Any, TranslatedException>)
    fun storeActivity(timer: Timer, activity: String, notes: String, cb: Promise<Boolean, Exception>)
    fun registerTimersUpdatedCallback(callback: TimersUpdatedCallback)
    fun unregisterTimersUpdatedCallback(callback: TimersUpdatedCallback)
    fun getNotes(timer: Timer): Notes
    fun setNotes(timer: Timer, notes: Notes?)
}
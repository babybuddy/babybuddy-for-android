package eu.pkgsoftware.babybuddywidgets.timers

import eu.pkgsoftware.babybuddywidgets.utils.Promise
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer

interface TimerControlInterface {
    fun startTimer(timer: Timer, cb: Promise<Timer, String>)
    fun stopTimer(timer: Timer, cb: Promise<Any, String>)
    fun storeActivity(timer: Timer, activity: String, notes: String, cb: Promise<Boolean, Exception>)
}
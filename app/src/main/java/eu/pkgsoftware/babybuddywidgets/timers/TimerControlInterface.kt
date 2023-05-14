package eu.pkgsoftware.babybuddywidgets.timers

import eu.pkgsoftware.babybuddywidgets.utils.Promise
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer

interface TimerControlInterface {
    fun startTimer(timerId: Int, cb: Promise<Timer, String>)
    fun stopTimer(timerId: Int, cb: Promise<Any, String>)
    fun storeActivity(timerId: Int, activity: String, cb: Promise<Any, String>)
}
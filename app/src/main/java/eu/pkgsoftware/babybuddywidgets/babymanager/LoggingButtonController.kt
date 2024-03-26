package eu.pkgsoftware.babybuddywidgets.babymanager

import android.os.Handler
import android.view.View
import android.widget.ImageButton
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.StoreFunction
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding
import eu.pkgsoftware.babybuddywidgets.databinding.DiaperLoggingEntryBinding
import eu.pkgsoftware.babybuddywidgets.databinding.GenericTimerLoggingEntryBinding
import eu.pkgsoftware.babybuddywidgets.login.Utils
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChangeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.SleepEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TummyTimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.classActivityName
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.nowServer
import eu.pkgsoftware.babybuddywidgets.timers.TimerControlInterface
import eu.pkgsoftware.babybuddywidgets.timers.TimersUpdatedCallback
import eu.pkgsoftware.babybuddywidgets.timers.TranslatedException
import eu.pkgsoftware.babybuddywidgets.utils.AsyncPromise
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import eu.pkgsoftware.babybuddywidgets.widgets.SwitchButtonLogic
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.io.IOException
import java.sql.Time
import java.util.Date
import kotlin.reflect.KClass

interface FragmentCallbacks {
    fun insertControls(view: View)
    fun removeControls(view: View)
    fun updateTimeline(newEntry: TimeEntry?)
}

abstract class LoggingControls(val childId: Int) {
    abstract val saveButton: ImageButton
    abstract val controlsView: View

    abstract fun storeStateForSuspend()
    abstract fun reset()
    suspend abstract fun save(): TimeEntry

    open fun updateVisuals() {}
}

val ACTIVITIES = listOf(
    BabyBuddyClient.EVENTS.CHANGE,
    BabyBuddyClient.EVENTS.NOTE,
    BabyBuddyClient.ACTIVITIES.SLEEP,
    BabyBuddyClient.ACTIVITIES.FEEDING,
    BabyBuddyClient.ACTIVITIES.TUMMY_TIME,
    BabyBuddyClient.ACTIVITIES.PUMPING,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiaperDataRecord(
    @JsonProperty("wet") val wet: Boolean,
    @JsonProperty("solid") val solid: Boolean,
    @JsonProperty("note") val note: String
)

class DiaperLoggingController(val fragment: BaseFragment, childId: Int) : LoggingControls(childId) {
    val bindings = DiaperLoggingEntryBinding.inflate(fragment.layoutInflater)
    override val controlsView = bindings.root
    override val saveButton = bindings.sendButton

    val wetLogic = SwitchButtonLogic(
        bindings.wetDisabledButton, bindings.wetEnabledButton, false
    )
    val solidLogic = SwitchButtonLogic(
        bindings.solidDisabledButton, bindings.solidEnabledButton, false
    )
    val noteEditor = bindings.noteEditor

    init {
        wetLogic.addStateListener { _, _ ->
            updateSaveEnabledState()
        }
        solidLogic.addStateListener { _, _ ->
            updateSaveEnabledState()
        }

        fragment.mainActivity.storage.child<DiaperDataRecord>(childId, "diaper")?.let {
            wetLogic.state = it.wet
            solidLogic.state = it.solid
            noteEditor.setText(it.note)
        }
    }

    fun updateSaveEnabledState() {
        saveButton.visibility = if (wetLogic.state || solidLogic.state) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    override fun storeStateForSuspend() {
        val ddr = DiaperDataRecord(
            wetLogic.state, solidLogic.state, noteEditor.text.toString()
        )
        fragment.mainActivity.storage.child(childId, "diaper", ddr)
    }

    override fun reset() {
        noteEditor.setText("")
        wetLogic.state = false
        solidLogic.state = false
    }

    suspend override fun save(): TimeEntry {
        return fragment.mainActivity.client.v2client.createEntry(
            ChangeEntry::class,
            ChangeEntry(
                id = 0,
                childId = childId,
                start = nowServer(),
                _notes = noteEditor.text.toString(),
                wet = wetLogic.state,
                solid = solidLogic.state,
                color = "",
                amount = null
            )
        )
    }
}

interface TimerBase {
    fun updateTimer(timer: BabyBuddyClient.Timer)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GenericTimerRecord(
    @JsonProperty("note") val note: String
)

abstract class GenericLoggingController(
    val fragment: BaseFragment,
    childId: Int,
    val timerControl: TimerControlInterface,
    val entryKlass: KClass<*>
) : LoggingControls(childId), TimerBase {
    protected abstract suspend fun createEntry(timer: Timer): TimeEntry

    val bindings = GenericTimerLoggingEntryBinding.inflate(fragment.layoutInflater)
    val typeName: String = classActivityName(entryKlass)

    override val saveButton: ImageButton = bindings.sendButton
    override val controlsView: View = bindings.root

    private var timer: Timer? = null

    init {
        fragment.mainActivity.storage.child<GenericTimerRecord>(childId, typeName)?.let {
            bindings.noteEditor.setText(it.note)
        }
    }

    override fun storeStateForSuspend() {
        fragment.mainActivity.storage.child(
            childId, typeName, GenericTimerRecord(bindings.noteEditor.text.toString())
        )
    }

    override fun reset() {
        bindings.noteEditor.setText("")
        storeStateForSuspend()
    }

    override suspend fun save(): TimeEntry {
        timer?.let { timer ->
            val result = AsyncPromise.call<TimeEntry?, Exception?> { promise ->
                fragment.mainActivity.storeActivity(
                    timer,
                    object : StoreFunction<TimeEntry> {
                        override fun store(
                            timer: Timer,
                            callback: BabyBuddyClient.RequestCallback<TimeEntry>
                        ) {
                            fragment.mainActivity.scope.launch {
                                try {
                                    val result = createEntry(timer)
                                    timerControl.stopTimer(
                                        timer,
                                        object : Promise<Any, TranslatedException> {
                                            override fun succeeded(s: Any?) {
                                                callback.response(result)
                                            }

                                            override fun failed(f: TranslatedException?) {
                                                callback.error(
                                                    f?.originalError
                                                        ?: IOException("Failed to stop timer")
                                                )
                                            }
                                        })
                                } catch (e: Exception) {
                                    callback.error(e)
                                }
                            }
                        }

                        override fun name(): String {
                            return this@GenericLoggingController.typeName
                        }

                        override fun timerStopped() {
                            timerControl.stopTimer(
                                timer,
                                object : Promise<Any, TranslatedException> {
                                    override fun succeeded(s: Any?) {
                                        promise.succeeded(null)
                                    }

                                    override fun failed(f: TranslatedException?) {
                                        promise.failed(f)
                                    }
                                })
                        }

                        override fun cancel() {
                            promise.failed(null)
                        }

                        override fun error(error: java.lang.Exception) {
                            promise.failed(error)
                        }

                        override fun response(response: TimeEntry?) {
                            promise.succeeded(response!!)
                        }

                    }
                )
            }
            result?.let {
                return it
            }
        }
        throw IOException("Could not store activity of type ${typeName}")
    }

    override fun updateTimer(timer: Timer) {
        this.timer = timer
    }

    override fun updateVisuals() {
        timer?.let {
            val diff = Date().time - it.start.time

            val seconds = diff.toInt() / 1000
            val minutes = seconds / 60
            val hours = minutes / 60

            bindings.currentTimerTime.text = "HH:MM:ss"
                .replace("HH".toRegex(), "" + hours)
                .replace("MM".toRegex(), Utils.padToLen("" + minutes % 60, '0', 2))
                .replace("ss".toRegex(), Utils.padToLen("" + seconds % 60, '0', 2))
        }
    }
}

class SleepLoggingController(
    fragment: BaseFragment,
    childId: Int,
    timerControl: TimerControlInterface
) : GenericLoggingController(fragment, childId, timerControl, SleepEntry::class) {
    override suspend fun createEntry(timer: Timer): TimeEntry {
        return fragment.mainActivity.client.v2client.createEntry(
            SleepEntry::class,
            SleepEntry(
                id = 0,
                childId = childId,
                start = timer.start,
                end = nowServer(),
                _notes = bindings.noteEditor.text.toString()
            )
        )
    }
}

class TummyTimeLoggingController(
    fragment: BaseFragment,
    childId: Int,
    timerControl: TimerControlInterface
) : GenericLoggingController(fragment, childId, timerControl, TummyTimeEntry::class) {
    override suspend fun createEntry(timer: Timer): TimeEntry {
        return fragment.mainActivity.client.v2client.createEntry(
            SleepEntry::class,
            SleepEntry(
                id = 0,
                childId = childId,
                start = timer.start,
                end = nowServer(),
                _notes = bindings.noteEditor.text.toString()
            )
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class LoggingButtonControllerStoreState(
    @JsonProperty("open_state") val openState: Array<String>,
)

class LoggingButtonController(
    val fragment: BaseFragment,
    val bindings: BabyManagerBinding,
    val controlsInterface: FragmentCallbacks,
    val child: BabyBuddyClient.Child,
    val timerControl: TimerControlInterface,
) : TimersUpdatedCallback {
    val logicMap = mapOf(
        BabyBuddyClient.EVENTS.CHANGE to SwitchButtonLogic(
            bindings.diaperDisabledButton, bindings.diaperEnabledButton, false
        ),
        BabyBuddyClient.EVENTS.NOTE to SwitchButtonLogic(
            bindings.notesDisabledButton, bindings.notesEnabledButton, false
        ),
        BabyBuddyClient.ACTIVITIES.SLEEP to SwitchButtonLogic(
            bindings.sleepDisabledButton, bindings.sleepEnabledButton, false
        ),
        BabyBuddyClient.ACTIVITIES.FEEDING to SwitchButtonLogic(
            bindings.feedingDisabledButton, bindings.feedingEnabledButton, false
        ),
        BabyBuddyClient.ACTIVITIES.TUMMY_TIME to SwitchButtonLogic(
            bindings.tummyTimeDisabledButton, bindings.tummyTimeEnabledButton, false
        ),
        BabyBuddyClient.ACTIVITIES.PUMPING to SwitchButtonLogic(
            bindings.pumpingDisabledButton, bindings.pumpingEnabledButton, false
        ),
    )

    val loggingControllers: Map<String, LoggingControls> = mapOf(
        BabyBuddyClient.EVENTS.CHANGE to DiaperLoggingController(fragment, child.id),
        BabyBuddyClient.ACTIVITIES.SLEEP to SleepLoggingController(
            fragment, child.id, timerControl
        ),
        BabyBuddyClient.ACTIVITIES.TUMMY_TIME to TummyTimeLoggingController(
            fragment, child.id, timerControl
        ),
    )

    private var timerHandler: Handler? = Handler(fragment.mainActivity.mainLooper)

    init {
        loggingControllers.forEach { (activity, controller) ->
            logicMap[activity]?.addStateListener { _, _ ->
                if (logicMap[activity]?.state == true) {
                    controlsInterface.insertControls(controller.controlsView)
                } else {
                    controlsInterface.removeControls(controller.controlsView)
                }
            }
            controller.saveButton.setOnClickListener {
                fragment.mainActivity.scope.launch {
                    runSave(activity, controller)
                }
            }

            timerControl.registerTimersUpdatedCallback(this)
        }

        fragment.mainActivity.storage.child<LoggingButtonControllerStoreState>(
            child.id, "loggingstate"
        )?.let {
            for ((k, logic) in logicMap.entries) {
                if (k !in BabyBuddyClient.EVENTS.ALL) continue
                if (k in it.openState) {
                    logic.state = true
                }
            }
        }

        timerHandler()
    }

    private fun timerHandler() {
        timerHandler?.let {
            it.postDelayed(Runnable { timerHandler() }, 500)
            for (c in loggingControllers.values) {
                c.updateVisuals()
            }
        }
    }

    suspend fun runSave(activity: String, controller: LoggingControls) {
        logicMap[activity]?.state = false
        val te = controller.save()
        controller.reset()
        storeStateForSuspend()
        controlsInterface.updateTimeline(te)
    }

    fun storeStateForSuspend() {
        val openState = mutableListOf<String>()
        for ((name, controller) in loggingControllers) {
            controller.storeStateForSuspend()
            if (name in BabyBuddyClient.EVENTS.ALL) {
                if (logicMap[name]?.state == true) {
                    openState.add(name)
                }
            }
        }
        fragment.mainActivity.storage.child(
            child.id,
            "loggingstate",
            LoggingButtonControllerStoreState(openState.toTypedArray())
        )
    }

    fun destroy() {
        storeStateForSuspend()
        for (controller in loggingControllers.values) {
            controlsInterface.removeControls(controller.controlsView)
        }
        for (logic in logicMap.values) {
            logic.destroy()
        }
        timerControl.unregisterTimersUpdatedCallback(this)
        timerHandler = null
    }

    override fun newTimerListLoaded(timers: Array<BabyBuddyClient.Timer>) {
        val toDisable =
            loggingControllers.filter { it.value is TimerBase }.map { it.key }.toMutableList()
        for (timer in timers) {
            if (!timer.active) continue
            loggingControllers[timer.name]?.let { controller ->
                if (controller is TimerBase) {
                    toDisable.remove(timer.name)

                    controller.updateTimer(timer)
                    controller.updateVisuals()
                }
            }
            logicMap[timer.name]?.let { logic ->
                logic.state = true
            }
        }
        for (name in toDisable) {
            logicMap[name]?.state = false
        }
    }
}
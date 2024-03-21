package eu.pkgsoftware.babybuddywidgets.babymanager

import android.view.View
import android.widget.ImageButton
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding
import eu.pkgsoftware.babybuddywidgets.databinding.DiaperLoggingEntryBinding
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.widgets.SwitchButtonLogic
import kotlinx.coroutines.launch

interface InsertRemoveControlsFunction {
    fun insertControls(view: View)
    fun removeControls(view: View)
}

abstract class LoggingControls(val childId: Int) {
    abstract val saveButton: ImageButton
    abstract val controlsView: View

    abstract fun storeStateForSuspend()
    abstract fun reset()
    suspend abstract fun save()
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
        saveButton.isEnabled = wetLogic.state || solidLogic.state
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

    suspend override fun save() {
        TODO("Not yet implemented")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class LoggingButtonControllerStoreState(
    @JsonProperty("open_state") val openState: Array<String>,
)

class LoggingButtonController(
    val fragment: BaseFragment,
    val bindings: BabyManagerBinding,
    val controlsInterface: InsertRemoveControlsFunction,
    val child: BabyBuddyClient.Child,
) {
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

    val diaperLogging = DiaperLoggingController(fragment, child.id)
    val loggingControllers = mapOf(
        BabyBuddyClient.EVENTS.CHANGE to diaperLogging
    )

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
                    runSave(controller)
                }
            }
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
    }

    suspend fun runSave(controller: LoggingControls) {
        controller.save()
        controller.reset()
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
        for ((name, controller) in loggingControllers) {
            controlsInterface.removeControls(controller.controlsView)
        }
        for ((name, logic) in logicMap) {
            logic.destroy()
        }
    }
}
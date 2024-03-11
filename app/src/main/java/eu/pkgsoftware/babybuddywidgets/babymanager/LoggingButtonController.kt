package eu.pkgsoftware.babybuddywidgets.babymanager

import android.view.View
import android.widget.ImageButton
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

abstract class LoggingControls {
    abstract val saveButton: ImageButton
    abstract val controlsView: View

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

class DiaperLoggingController(val fragment: BaseFragment) : LoggingControls() {
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
    }

    fun updateSaveEnabledState() {
        saveButton.isEnabled = wetLogic.state || solidLogic.state
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

class LoggingButtonController(
    val fragment: BaseFragment,
    val bindings: BabyManagerBinding,
    val controlsInterface: InsertRemoveControlsFunction
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

    val diaperLogging = DiaperLoggingController(fragment)
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
    }

    suspend fun runSave(controller: LoggingControls) {
        controller.save()
        controller.reset()
    }
}
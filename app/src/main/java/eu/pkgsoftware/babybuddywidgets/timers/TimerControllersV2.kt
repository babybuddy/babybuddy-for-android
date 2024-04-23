package eu.pkgsoftware.babybuddywidgets.timers

import android.os.Handler
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.core.view.children
import androidx.core.view.isVisible
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.phrase.Phrase
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.Constants
import eu.pkgsoftware.babybuddywidgets.Constants.FeedingMethodEnum
import eu.pkgsoftware.babybuddywidgets.Constants.FeedingTypeEnum
import eu.pkgsoftware.babybuddywidgets.Constants.FeedingTypeEnumValues
import eu.pkgsoftware.babybuddywidgets.DialogCallback
import eu.pkgsoftware.babybuddywidgets.FeedingFragment.ButtonListCallback
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.StoreFunction
import eu.pkgsoftware.babybuddywidgets.databinding.BabyManagerBinding
import eu.pkgsoftware.babybuddywidgets.databinding.DiaperLoggingEntryBinding
import eu.pkgsoftware.babybuddywidgets.databinding.FeedingLoggingEntryBinding
import eu.pkgsoftware.babybuddywidgets.databinding.GenericTimerLoggingEntryBinding
import eu.pkgsoftware.babybuddywidgets.databinding.NoteLoggingEntryBinding
import eu.pkgsoftware.babybuddywidgets.login.Utils
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChangeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.FeedingEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.NoteEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.SleepEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TummyTimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.classActivityName
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.nowServer
import eu.pkgsoftware.babybuddywidgets.utils.AsyncPromise
import eu.pkgsoftware.babybuddywidgets.utils.AsyncPromiseFailure
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import eu.pkgsoftware.babybuddywidgets.widgets.SwitchButtonLogic
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.io.IOException
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
    suspend abstract fun save(): TimeEntry?

    open fun updateVisuals() {}
    open fun postInit() {}
}

val ACTIVITIES = listOf(
    BabyBuddyClient.EVENTS.CHANGE,
    BabyBuddyClient.EVENTS.NOTE,
    BabyBuddyClient.ACTIVITIES.SLEEP,
    BabyBuddyClient.ACTIVITIES.FEEDING,
    BabyBuddyClient.ACTIVITIES.TUMMY_TIME,
    BabyBuddyClient.ACTIVITIES.PUMPING,
)

interface TimerBase {
    fun updateTimer(timer: Timer?)
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
) : LoggingControls(childId), TimerBase, StoreFunction<TimeEntry> {
    protected abstract suspend fun createEntry(timer: Timer): TimeEntry

    val bindings = GenericTimerLoggingEntryBinding.inflate(fragment.layoutInflater)
    val typeName: String = classActivityName(entryKlass)

    open val uiIconList = bindings.icons.children
    open val uiNoteEditor = bindings.noteEditor
    open val uiCurrentTimerTime = bindings.currentTimerTime

    override val saveButton: ImageButton = bindings.sendButton
    override val controlsView: View = bindings.root

    private var timer: Timer? = null
    private var storingPromise: Promise<TimeEntry?, Exception>? = null

    override fun postInit() {
        fragment.mainActivity.storage.child<GenericTimerRecord>(childId, typeName)?.let {
            uiNoteEditor.setText(it.note)
        }

        val children = uiIconList.toList()
        for (i in BabyBuddyClient.ACTIVITIES.ALL.indices) {
            if (BabyBuddyClient.ACTIVITIES.ALL[i] == typeName) {
                children[i].visibility = View.VISIBLE
            } else {
                children[i].visibility = View.GONE
            }
        }

        updateVisuals()
    }

    override fun storeStateForSuspend() {
        fragment.mainActivity.storage.child(
            childId, typeName, GenericTimerRecord(uiNoteEditor.text.toString())
        )
    }

    override fun reset() {
        uiNoteEditor.setText("")
        storeStateForSuspend()
    }

    override suspend fun save(): TimeEntry? {
        storingPromise?.let {
            throw IOException("Already storing activity of type ${typeName}")
        }

        timer?.let { timer ->
            try {
                try {
                    val result = AsyncPromise.call<TimeEntry?, Exception> { promise ->
                        storingPromise = promise
                        fragment.mainActivity.storeActivity(timer,this)
                    }
                    return result
                }
                finally {
                    storingPromise = null
                }
            }
            catch (e: AsyncPromiseFailure) {
                fragment.showError(
                    true,
                    R.string.activity_store_failure_message,
                    R.string.activity_store_failure_server_error
                )
            }
        }
        throw IOException("Could not store activity of type ${typeName}")
    }

    override fun updateTimer(timer: Timer?) {
        this.timer = timer
        updateVisuals()
    }

    override fun updateVisuals() {
        val now = Date()
        (timer?.start ?: now).let {
            val diff = now.time - it.time

            val seconds = diff.toInt() / 1000
            val minutes = seconds / 60
            val hours = minutes / 60

            uiCurrentTimerTime.text = "HH:MM:ss"
                .replace("HH".toRegex(), "" + hours)
                .replace("MM".toRegex(), Utils.padToLen("" + minutes % 60, '0', 2))
                .replace("ss".toRegex(), Utils.padToLen("" + seconds % 60, '0', 2))
        }
    }

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

    override fun stopTimer(timer: Timer) {
        timerControl.stopTimer(
            timer,
            object : Promise<Any, TranslatedException> {
                override fun succeeded(s: Any?) {
                    storingPromise!!.succeeded(null)
                }

                override fun failed(f: TranslatedException?) {
                    storingPromise!!.failed(f)
                }
            })
    }

    override fun cancel() {
        storingPromise!!.succeeded(null)
    }

    override fun error(error: java.lang.Exception) {
        var message = "" + (error.message ?: "")
        if ((error is RequestCodeFailure) && (error.hasJSONMessage())) {
            message = Phrase.from(
                fragment.requireContext(),
                R.string.activity_store_failure_server_error
            )
                .put("message", "Error while storing activity")
                .put("server_message", error.jsonErrorMessages().joinToString(", "))
                .format().toString()
        }

        fragment.showQuestion(
            true,
            fragment.getString(R.string.activity_store_failure_message),
            message,
            fragment.getString(R.string.activity_store_failure_cancel),
            fragment.getString(R.string.activity_store_failure_stop_timer),
            object : DialogCallback {
                override fun call(b: Boolean) {
                    if (!b) {
                        timer?.let { stopTimer(it) }
                    } else {
                        storingPromise!!.succeeded(null)
                    }
                }
            }
        )
    }

    override fun response(response: TimeEntry?) {
        storingPromise!!.succeeded(response)
    }
}

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

        updateSaveEnabledState()
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotesDataRecord(
    @JsonProperty("note") val note: String
)

class NotesLoggingController(val fragment: BaseFragment, childId: Int) : LoggingControls(childId) {
    val bindings = NoteLoggingEntryBinding.inflate(fragment.layoutInflater)
    override val controlsView = bindings.root
    override val saveButton = bindings.sendButton

    val noteEditor = bindings.noteEditor

    init {
        fragment.mainActivity.storage.child<DiaperDataRecord>(childId, "notes")?.let {
            noteEditor.setText(it.note)
        }
    }

    override fun storeStateForSuspend() {
        val ddr = NotesDataRecord(noteEditor.text.toString())
        fragment.mainActivity.storage.child(childId, "notes", ddr)
    }

    override fun reset() {
        noteEditor.setText("")
    }

    suspend override fun save(): TimeEntry {
        return fragment.mainActivity.client.v2client.createEntry(
            NoteEntry::class,
            NoteEntry(
                id = 0,
                childId = childId,
                start = nowServer(),
                _notes = noteEditor.text.toString()
            )
        )
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
            TummyTimeEntry::class,
            TummyTimeEntry(
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
data class FeedingRecord(
    @JsonProperty("amount") val amount: Int?,
    @JsonProperty("note") val note: String,
    @JsonProperty("feeding_type") val feedingType: String?,
    @JsonProperty("feeding_method") val feedingMethod: String?,
)

class FeedingLoggingController(
    fragment: BaseFragment,
    childId: Int,
    timerControl: TimerControlInterface
) : GenericLoggingController(fragment, childId, timerControl, TummyTimeEntry::class) {
    val feedingBinding = FeedingLoggingEntryBinding.inflate(fragment.layoutInflater)

    override val uiCurrentTimerTime = feedingBinding.currentTimerTime
    override val uiNoteEditor = feedingBinding.noteEditor
    override val saveButton: ImageButton = feedingBinding.sendButton
    override val controlsView: View = feedingBinding.root

    private var assignedMethodButtons: List<FeedingMethodEnum> = emptyList()
    private var selectedType: String? = null
    private var selectedMethod: String? = null

    override fun postInit() {
        super.postInit()

        populateButtonList(
            fragment.resources.getTextArray(R.array.feedingTypes),
            feedingBinding.feedingTypeButtons,
            feedingBinding.feedingTypeSpinner
        ) { i ->
            selectedType = FeedingTypeEnumValues[i]!!.post_name
            selectedMethod = null
            setupFeedingMethodButtons(FeedingTypeEnumValues[i]!!)
            feedingBinding.feedingMethodButtons.visibility = View.VISIBLE
            feedingBinding.feedingMethodSpinner.visibility = View.GONE
            updateVisuals()
        }
        feedingBinding.feedingTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newType = FeedingTypeEnumValues[position]!!.post_name
                if (newType == selectedType) return
                selectedType = newType
                selectedMethod = null
                setupFeedingMethodButtons(FeedingTypeEnumValues[position]!!)
                feedingBinding.feedingMethodButtons.visibility = View.VISIBLE
                feedingBinding.feedingMethodSpinner.visibility = View.GONE
                updateVisuals()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                feedingBinding.feedingMethodSpinner.visibility = View.GONE
                feedingBinding.feedingMethodButtons.visibility = View.GONE
            }
        }

        feedingBinding.amountNumberPicker.value = null

        fragment.mainActivity.storage.child<FeedingRecord>(childId, "feeding")?.let {
            feedingBinding.amountNumberPicker.value = it.amount
            feedingBinding.noteEditor.setText(it.note)

            selectedType = null
            selectedMethod = null

            it.feedingType?.let {
                try {
                    feedingBinding.feedingTypeSpinner.setSelection(FeedingTypeEnum.byPostName(it).value)
                    selectedType = it
                    feedingBinding.feedingTypeButtons.visibility = View.GONE
                    feedingBinding.feedingTypeSpinner.visibility = View.VISIBLE
                    setupFeedingMethodButtons(FeedingTypeEnum.byPostName(it))
                }
                catch (_: NoSuchElementException) {}
            }
            it.feedingMethod?.let {
                if (selectedType != null) {
                    try {
                        assignedMethodButtons.indexOf(FeedingMethodEnum.byPostName(it)).let {
                            feedingBinding.feedingMethodSpinner.setSelection(it)
                        }
                        selectedMethod = it
                        feedingBinding.feedingMethodButtons.visibility = View.GONE
                        feedingBinding.feedingMethodSpinner.visibility = View.VISIBLE
                    }
                    catch (_: NoSuchElementException) {}
                }
            }
        }

        updateVisuals()
    }

    override fun storeStateForSuspend() {
        val fr = FeedingRecord(
            feedingBinding.amountNumberPicker.value,
            feedingBinding.noteEditor.text.toString(),
            selectedType,
            selectedMethod,
        )
        fragment.mainActivity.storage.child(childId, "feeding", fr)
    }

    override fun reset() {
        feedingBinding.amountNumberPicker.value = null
        feedingBinding.noteEditor.setText("")
    }

    override fun updateVisuals() {
        super.updateVisuals()

        if (feedingBinding.feedingTypeSpinner.isVisible && feedingBinding.feedingMethodSpinner.isVisible) {
            saveButton.visibility = View.VISIBLE
        } else {
            saveButton.visibility = View.INVISIBLE
        }
    }

    override suspend fun createEntry(timer: Timer): TimeEntry {
        return fragment.mainActivity.client.v2client.createEntry(
            FeedingEntry::class,
            FeedingEntry(
                id = 0,
                childId = childId,
                start = timer.start,
                end = nowServer(),
                feedingType = selectedType!!,
                feedingMethod = selectedMethod!!,
                amount = feedingBinding.amountNumberPicker.value?.toDouble(),
                _notes = bindings.noteEditor.text.toString()
            )
        )
    }

    private fun populateButtonList(
        textArray: Array<CharSequence>,
        buttons: LinearLayout,
        spinner: Spinner,
        callback: ButtonListCallback
    ) {
        spinner.visibility = View.GONE
        buttons.visibility = View.VISIBLE
        buttons.removeAllViewsInLayout()
        for (i in textArray.indices) {
            val button = Button(fragment.requireContext())
            button.setOnClickListener {
                spinner.setSelection(i)
                spinner.visibility = View.VISIBLE
                buttons.visibility = View.GONE
                callback.onSelectionChanged(i)
            }
            button.text = textArray[i]
            button.setLayoutParams(
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
            buttons.addView(button)
        }
    }

    private fun setupFeedingMethodButtons(type: FeedingTypeEnum) {
        when (type) {
            FeedingTypeEnum.BREAST_MILK -> {
                assignedMethodButtons = listOf(
                    FeedingMethodEnum.LEFT_BREAST,
                    FeedingMethodEnum.RIGHT_BREAST,
                    FeedingMethodEnum.BOTH_BREASTS,
                    FeedingMethodEnum.BOTTLE,
                    FeedingMethodEnum.PARENT_FED,
                    FeedingMethodEnum.SELF_FED,
                )
            }
            else -> {
                assignedMethodButtons = listOf(
                    FeedingMethodEnum.BOTTLE,
                    FeedingMethodEnum.PARENT_FED,
                    FeedingMethodEnum.SELF_FED,
                )
            }
        }

        val orgItems: Array<CharSequence> = fragment.resources.getTextArray(R.array.feedingMethods)
        val textItems: MutableList<CharSequence> = ArrayList(10)
        for (i in assignedMethodButtons.indices) {
            textItems.add(orgItems[assignedMethodButtons.get(i).value])
        }
        feedingBinding.feedingMethodSpinner.setAdapter(
            ArrayAdapter<CharSequence>(
                fragment.requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                textItems
            )
        )
        populateButtonList(
            textItems.toTypedArray<CharSequence>(),
            feedingBinding.feedingMethodButtons,
            feedingBinding.feedingMethodSpinner
        ) {
            selectedMethod = assignedMethodButtons[it].post_name
            updateVisuals()
        }
        feedingBinding.feedingMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMethod = assignedMethodButtons[position].post_name
                updateVisuals()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedMethod = null
                updateVisuals()
            }
        }
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
        BabyBuddyClient.EVENTS.NOTE to NotesLoggingController(fragment, child.id),
        BabyBuddyClient.ACTIVITIES.SLEEP to SleepLoggingController(
            fragment, child.id, timerControl
        ),
        BabyBuddyClient.ACTIVITIES.TUMMY_TIME to TummyTimeLoggingController(
            fragment, child.id, timerControl
        ),
        BabyBuddyClient.ACTIVITIES.FEEDING to FeedingLoggingController(
            fragment, child.id, timerControl
        ),
    )

    private var timerHandler: Handler? = Handler(fragment.mainActivity.mainLooper)
    private var cachedTimers = emptyArray<Timer>()
    private var runningTimerMods = 0

    init {
        loggingControllers.forEach { (activity, controller) ->
            controller.postInit()

            logicMap[activity]?.addStateListener { state, userInduced ->
                if (state) {
                    controlsInterface.insertControls(controller.controlsView)
                    if (userInduced && (controller is TimerBase)) {
                        cachedTimers.firstOrNull { it.name == activity }?.let {
                            runningTimerMods += 1
                            timerControl.startTimer(
                                it,
                                object : Promise<Timer, TranslatedException> {
                                    override fun succeeded(t: Timer) {
                                        runningTimerMods -= 1
                                    }

                                    override fun failed(f: TranslatedException?) {
                                        runningTimerMods -= 1
                                        newTimerListLoaded(cachedTimers)
                                    }
                                })
                        }
                    }
                } else {
                    controlsInterface.removeControls(controller.controlsView)
                    if (userInduced && (controller is TimerBase)) {
                        controller.updateTimer(null)
                        cachedTimers.firstOrNull { it.name == activity }?.let {
                            runningTimerMods += 1
                            timerControl.stopTimer(
                                it,
                                object : Promise<Any, TranslatedException> {
                                    override fun succeeded(s: Any?) {
                                        runningTimerMods -= 1
                                    }

                                    override fun failed(f: TranslatedException?) {
                                        runningTimerMods -= 1
                                        newTimerListLoaded(cachedTimers)
                                    }
                                })
                        }
                    }
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

    override fun newTimerListLoaded(timers: Array<Timer>) {
        cachedTimers = timers;
        if (runningTimerMods > 0) return

        val toDisable =
            loggingControllers.filter { it.value is TimerBase }.map { it.key }.toMutableList()
        for (timer in timers) {
            if (!timer.active) continue
            loggingControllers[timer.name]?.let { controller ->
                if (controller is TimerBase) {
                    toDisable.remove(timer.name)

                    controller.updateTimer(timer)
                    controller.updateVisuals()
                    controlsInterface.insertControls(controller.controlsView)
                }
            }
            logicMap[timer.name]?.let { logic ->
                logic.state = true
            }
        }
        for (name in toDisable) {
            logicMap[name]?.state = false
            loggingControllers[name]?.let {
                controlsInterface.removeControls(it.controlsView)
            }
        }
    }
}
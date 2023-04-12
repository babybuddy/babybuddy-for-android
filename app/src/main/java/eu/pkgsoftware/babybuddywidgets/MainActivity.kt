package eu.pkgsoftware.babybuddywidgets

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.Navigation
import com.squareup.phrase.Phrase
import eu.pkgsoftware.babybuddywidgets.databinding.ActivityMainBinding
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.GenericSubsetResponseHeader
import kotlinx.coroutines.*
import org.json.JSONArray
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface StoreFunction<X> : BabyBuddyClient.RequestCallback<X> {
    fun store(timer: BabyBuddyClient.Timer, callback: BabyBuddyClient.RequestCallback<X>)
    fun name(): String
    fun timerStopped()
    fun cancel()
}


enum class ConflictResolutionOptions {
    CANCEL, RESOLVE, STOP_TIMER
}

class AsyncClientRequest() {
    companion object {
        suspend inline fun <X> call(crossinline body: (callback: BabyBuddyClient.RequestCallback<X>) -> Unit): X {
            return withContext(Dispatchers.Default) {
                suspendCoroutine<X> { continuation ->
                    val callbacks = object : BabyBuddyClient.RequestCallback<X> {
                        override fun error(error: Exception) {
                            continuation.resumeWithException(error)
                        }

                        override fun response(response: X) {
                            continuation.resume(response)
                        }
                    }
                    body.invoke(callbacks)
                }
            }
        }
    }
}

fun interface InputEventListener {
    fun inputEvent(event: InputEvent)
}

class MainActivity : AppCompatActivity() {
    val scope = MainScope()

    private var binding: ActivityMainBinding? = null
    val inputEventListeners = mutableListOf<InputEventListener>()

    internal var internalCredStore: CredStore? = null
    val credStore: CredStore
        get() {
            internalCredStore.let {
                if (it == null) {
                    val newCredStore = CredStore(applicationContext)
                    internalCredStore = newCredStore
                    return newCredStore
                } else {
                    return it
                }
            }
        }

    internal var internalClient: BabyBuddyClient? = null
    val client: BabyBuddyClient
        get() {
            internalClient.let {
                if (it == null) {
                    val newClient = BabyBuddyClient(
                        mainLooper,
                        credStore
                    )
                    internalClient = newClient
                    return newClient
                } else {
                    return it
                }
            }
        }

    internal var internalTutorialAccess: TutorialAccess? = null
    val tutorialAccess: TutorialAccess
        get() {
            internalTutorialAccess.let {
                if (it == null) {
                    val newTA = TutorialAccess(this)
                    internalTutorialAccess = newTA
                    return newTA
                } else {
                    return it
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(
            layoutInflater
        ).let {
            setContentView(it.root)
            setSupportActionBar(it.toolbar)
            it
        }
        enableBackNavigationButton(false)
        tutorialAccess
    }

    override fun onStart() {
        super.onStart()

        binding?.root?.let {
            val ncv = it.findViewById<FragmentContainerView>(R.id.nav_host_fragment_content_main)
            Navigation.findNavController(ncv)
                .addOnDestinationChangedListener { controller, destination, arguments ->
                    enableBackNavigationButton(false)
                }
        }
    }

    @JvmField
    var children = arrayOf<Child>()

    @JvmField
    var selectedTimer: BabyBuddyClient.Timer? = null

    fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    fun enableBackNavigationButton(b: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(b)
        supportActionBar?.setDisplayShowHomeEnabled(b)
    }

    override fun onSupportNavigateUp(): Boolean {
        binding?.root?.let {
            val ncv = it.findViewById<FragmentContainerView>(R.id.nav_host_fragment_content_main)
            Navigation.findNavController(ncv).navigateUp()
        }
        return false;
    }

    private fun invokeInputEventListeners(e: InputEvent) {
        inputEventListeners.forEach {
            it.inputEvent(e)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ev?.let {
            invokeInputEventListeners(it)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(ev: KeyEvent?): Boolean {
        ev?.let {
            invokeInputEventListeners(it)
        }
        return super.dispatchKeyEvent(ev)
    }

    fun <X> storeActivity(
        timer: BabyBuddyClient.Timer,
        storeInterface: StoreFunction<X>
    ) {
        suspend fun trySave(): X {
            return AsyncClientRequest.call {
                storeInterface.store(timer, it)
            }
        }

        suspend fun listConflicts(): List<BabyBuddyClient.TimeEntry> {
            val jsonResponse = AsyncClientRequest.call<GenericSubsetResponseHeader<JSONArray>> {

                client.listGeneric(
                    storeInterface.name(),
                    0,
                    BabyBuddyClient.QueryValues()
                        .add("start_max", timer.computeCurrentServerEndTime(client))
                        .add("end_min", timer.start)
                        .add("limit", 50),
                    it
                )
            }
            val result = ArrayList<BabyBuddyClient.TimeEntry>()
            for (i in 0..jsonResponse.payload.length() - 1) {
                try {
                    val entry = BabyBuddyClient.TimeEntry.fromJsonObject(
                        jsonResponse.payload.getJSONObject(i), storeInterface.name()
                    )
                    result.add(entry)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Received unparsable json object")
                }
            }
            return result
        }

        suspend fun askForResolutionMethod(): ConflictResolutionOptions {
            return suspendCoroutine<ConflictResolutionOptions> { continuation ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.conflicting_activity_title)
                    .setMessage(R.string.conflicting_activity_text)
                    .setCancelable(false)
                    .setPositiveButton(R.string.conflicting_activity_modify_option) { a, b ->
                        continuation.resume(ConflictResolutionOptions.RESOLVE)
                    }
                    .setNeutralButton(R.string.conflicting_activity_cancel_option) { a, b ->
                        continuation.resume(ConflictResolutionOptions.CANCEL)
                    }
                    .setNegativeButton(R.string.conflicting_activity_stop_timer_option) { a, b ->
                        continuation.resume(ConflictResolutionOptions.STOP_TIMER)
                    }
                    .show()
            }
        }

        suspend fun stopTimer() {
            AsyncClientRequest.call<Boolean> {
                client.setTimerActive(timer.id, false, it)
            }
        }

        suspend fun patchEntry(
            e: BabyBuddyClient.TimeEntry,
            values: BabyBuddyClient.QueryValues
        ): BabyBuddyClient.TimeEntry {
            return AsyncClientRequest.call {
                client.updateTimelineEntry(e, values, it)
            }
        }

        suspend fun resolve(conflicts: List<BabyBuddyClient.TimeEntry>) {
            var anyException: Exception? = null
            val endTime = timer.computeCurrentServerEndTime(client)
            for (c in conflicts) {
                val values = BabyBuddyClient.QueryValues()
                if (c.start.time < timer.start.time) {
                    values.add("end", timer.start)
                } else if (c.end.time > endTime.time) {
                    values.add("start", endTime)
                } else {
                    val startDistance = Math.abs(c.start.time - timer.start.time)
                    val endDistance = Math.abs(c.end.time - endTime.time)
                    val adjustTimeTo = if (startDistance <= endDistance) timer.start else endTime
                    values.add("start", adjustTimeTo)
                    values.add("end", adjustTimeTo)
                }
                try {
                    patchEntry(c, values)
                } catch (e: Exception) {
                    anyException = e
                }
            }
            if (anyException != null) {
                throw anyException;
            }
        }

        @Suppress("DEPRECATION")
        scope.launch {
            var readableActivityName = storeInterface.name()
            if (BabyBuddyClient.ACTIVITIES.index(readableActivityName) >= 0) {
                val i = BabyBuddyClient.ACTIVITIES.index(readableActivityName);
                readableActivityName = resources.getStringArray(R.array.timerTypes).get(i)
            }

            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog.setMessage(
                Phrase.from(applicationContext, R.string.popup_message_storing)
                    .putOptional("activity", readableActivityName)
                    .format()
            )

            progressDialog.show()
            try {
                var conflicts = listOf<BabyBuddyClient.TimeEntry>()
                try {
                    val result = trySave()
                    storeInterface.response(result)
                } catch (e: Exception) {
                    conflicts = listConflicts()
                    if (conflicts.isEmpty()) {
                        progressDialog.cancel()
                        throw e
                    }
                }
                if (conflicts.isEmpty()) {
                    progressDialog.cancel()
                    return@launch
                }

                progressDialog.hide()
                val resolution = askForResolutionMethod()
                progressDialog.show()
                if (resolution == ConflictResolutionOptions.STOP_TIMER) {
                    progressDialog.cancel()
                    stopTimer()
                    storeInterface.timerStopped()
                } else if (resolution == ConflictResolutionOptions.RESOLVE) {
                    stopTimer()
                    var retries = 3
                    while (retries > 0) {
                        resolve(conflicts)
                        try {
                            storeInterface.response(trySave())
                            return@launch
                        } catch (e: Exception) {
                            conflicts = listConflicts()
                        }
                        retries--
                        delay(1000)
                    }
                    storeInterface.error(java.lang.Exception("Failed to correct conflicting time entries"))
                } else {
                    progressDialog.cancel()
                    storeInterface.cancel()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                storeInterface.error(e)
            } finally {
                progressDialog.cancel()
            }
        }
    }
}
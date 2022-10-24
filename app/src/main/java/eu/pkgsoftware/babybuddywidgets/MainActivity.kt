package eu.pkgsoftware.babybuddywidgets

import androidx.appcompat.app.AppCompatActivity
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import eu.pkgsoftware.babybuddywidgets.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
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

class MainActivity : AppCompatActivity() {
    val scope = MainScope()

    private var binding: ActivityMainBinding? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(
            layoutInflater
        ).let {
            setContentView(it.root)
            setSupportActionBar(it.toolbar)
            it.toolbar.setNavigationOnClickListener { view: View? -> }
            it.toolbar.navigationIcon = null
            it
        }
    }

    @JvmField
    var children = arrayOf<Child>()

    @JvmField
    var selectedTimer: BabyBuddyClient.Timer? = null

    fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    fun <X> storeActivity(
        timer: BabyBuddyClient.Timer,
        storeInterface: StoreFunction<X>
    ) {
        val MINUTE = 60 * 1000L
        val rawEndDate = timer.computeCurrentServerEndTime(client)
        val roundedStart = Date(timer.start.time - timer.start.time % MINUTE)
        val roundedEnd = Date(rawEndDate.time - rawEndDate.time % MINUTE + MINUTE)

        suspend fun trySave(): X {
            return AsyncClientRequest.call {
                storeInterface.store(timer, it)
            }
        }

        suspend fun listConflicts(): List<BabyBuddyClient.TimeEntry> {
            val jsonEntries = AsyncClientRequest.call<JSONArray> {

                client.listGeneric(
                    storeInterface.name(),
                    BabyBuddyClient.QueryValues()
                        .add("start_max", roundedEnd)
                        .add("end_min", roundedStart)
                        .add("limit", 50),
                    it
                )
            }
            val result = ArrayList<BabyBuddyClient.TimeEntry>()
            for (i in 0..jsonEntries.length() - 1) {
                try {
                    val entry = BabyBuddyClient.TimeEntry.fromJsonObject(
                        jsonEntries.getJSONObject(i), storeInterface.name()
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
            for (c in conflicts) {
                val values = BabyBuddyClient.QueryValues()
                if (c.start.time < roundedStart.time) {
                    values.add("end", roundedStart)
                } else if (c.end.time > roundedEnd.time) {
                    values.add("start", roundedEnd)
                } else {
                    val startDistance = Math.abs(c.start.time - roundedStart.time)
                    val endDistance = Math.abs(c.end.time - roundedEnd.time)
                    val adjustTimeTo = if (startDistance <= endDistance) roundedStart else roundedEnd
                    values.add("start", adjustTimeTo)
                    values.add("end", adjustTimeTo)
                }
                patchEntry(c, values)
            }
        }

        scope.launch {
            try {
                var conflicts = listOf<BabyBuddyClient.TimeEntry>()
                try {
                    trySave()
                } catch (e: Exception) {
                    conflicts = listConflicts()
                    if (conflicts.isEmpty()) {
                        throw e
                    }
                }
                if (conflicts.isEmpty()) {
                    return@launch
                }

                val resolution = askForResolutionMethod()
                if (resolution == ConflictResolutionOptions.STOP_TIMER) {
                    stopTimer()
                    storeInterface.timerStopped()
                } else if (resolution == ConflictResolutionOptions.RESOLVE) {
                    var retries = 3
                    while (retries > 0) {
                        resolve(conflicts)
                        try {
                            storeInterface.response(trySave())
                            return@launch
                        } catch (e: Exception) {
                            if (retries == 0) {
                                storeInterface.error(e)
                                return@launch
                            }
                            conflicts = listConflicts()
                        }
                        retries--
                    }
                } else {
                    storeInterface.cancel()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                storeInterface.error(e)
            }
        }
    }
}
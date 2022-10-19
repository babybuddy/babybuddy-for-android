package eu.pkgsoftware.babybuddywidgets

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Child
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import eu.pkgsoftware.babybuddywidgets.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.json.JSONArray
import java.lang.Exception
import java.util.*
import kotlin.coroutines.CoroutineContext
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
        suspend fun trySave(): X {
            return AsyncClientRequest.call {
                storeInterface.store(timer, it)
            }
        }


        suspend fun hasConflict(): Boolean {
            val entries = AsyncClientRequest.call<JSONArray> {
                val endDate = timer.computeCurrentServerEndTime(client)
                client.listGeneric(
                    storeInterface.name(),
                    BabyBuddyClient.Filters()
                        .add("start_max", endDate)
                        .add("end_min", timer.start)
                        .add("limit", 10),
                    it
                )
            }
            return entries.length() > 0
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

        suspend fun resolve() {
            // TODO...
        }


        scope.launch {
            try {
                try {
                    trySave()
                } catch (e: Exception) {
                    if (!hasConflict()) {
                        throw e
                    }
                }

                val resolution = askForResolutionMethod()
                if (resolution == ConflictResolutionOptions.STOP_TIMER) {
                    stopTimer()
                    storeInterface.timerStopped()
                } else if (resolution == ConflictResolutionOptions.RESOLVE) {
                    resolve()
                    storeInterface.response(trySave())
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
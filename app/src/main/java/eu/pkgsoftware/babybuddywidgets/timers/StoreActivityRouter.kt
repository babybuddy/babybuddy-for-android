package eu.pkgsoftware.babybuddywidgets.timers

import eu.pkgsoftware.babybuddywidgets.MainActivity
import eu.pkgsoftware.babybuddywidgets.StoreFunction
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.RequestCallback
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.Timer
import eu.pkgsoftware.babybuddywidgets.utils.AsyncClientRequest
import eu.pkgsoftware.babybuddywidgets.utils.Promise
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class InvalidActivityException : java.lang.Exception() {}

class StoreActivityRouter(val mainActivity: MainActivity) {
    private val client = mainActivity.client

    private abstract inner class DeferredStoreFunction(
        val activityName: String,
        val timer: Timer,
        val suspensionScope: Continuation<Boolean>
    ) : StoreFunction<Boolean> {
        override fun name(): String {
            return activityName
        }

        override fun timerStopped() {
            mainActivity.scope.launch {
                try {
                    AsyncClientRequest.call<Boolean> {
                        client.deleteTimer(timer.id, it)
                    }
                } catch (e: java.lang.Exception) {
                    suspensionScope.resumeWithException(e)
                    return@launch
                }
                suspensionScope.resume(false)
            }
        }

        override fun cancel() {
            suspensionScope.resume(false)
        }

        override fun error(error: java.lang.Exception) {
            suspensionScope.resumeWithException(error)
        }

        override fun response(response: Boolean?) {
            suspensionScope.resume(response ?: false)
        }
    }

    private interface SimpleStoreFunctionInterface {
        fun store(callback: RequestCallback<Boolean>)
    }

    private inner class SimpleStoreFunction(
        activityName: String,
        timer: Timer,
        suspensionScope: Continuation<Boolean>,
        val func: SimpleStoreFunctionInterface
    ) : DeferredStoreFunction(activityName, timer, suspensionScope) {
        override fun store(timer: Timer, callback: RequestCallback<Boolean>) {
            func.store(callback)
        }
    }

    suspend fun asyncStore(activity: String, notes: String, timer: Timer): Boolean {
        return suspendCoroutine<Boolean> { continuation ->
            val storeInterface = when (activity) {
                ACTIVITIES.SLEEP -> SimpleStoreFunction(
                    activity,
                    timer,
                    continuation,
                    object : SimpleStoreFunctionInterface {
                        override fun store(callback: RequestCallback<Boolean>) {
                            client.createSleepRecordFromTimer(timer, notes, callback)
                        }
                    }
                )

                ACTIVITIES.TUMMY_TIME -> SimpleStoreFunction(
                    activity,
                    timer,
                    continuation,
                    object : SimpleStoreFunctionInterface {
                        override fun store(callback: RequestCallback<Boolean>) {
                            client.createTummyTimeRecordFromTimer(timer, notes, callback)
                        }
                    }
                )

                ACTIVITIES.FEEDING -> throw java.lang.Exception("NOT IMPLEMENTED YET")
                else -> {
                    continuation.resumeWithException(InvalidActivityException())
                    return@suspendCoroutine
                }
            }
            mainActivity.storeActivity(timer, storeInterface)
        }
    }

    fun store(
        activity: String,
        notes: String,
        timer: Timer,
        promise: Promise<Boolean, java.lang.Exception>
    ) {
        mainActivity.scope.launch {
            val result: Boolean
            try {
                result = asyncStore(activity, notes, timer)
            } catch (e: java.lang.Exception) {
                promise.failed(e)
                return@launch
            }
            promise.succeeded(result)
        }
    }
}
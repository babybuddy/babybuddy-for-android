package eu.pkgsoftware.babybuddywidgets.utils

import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

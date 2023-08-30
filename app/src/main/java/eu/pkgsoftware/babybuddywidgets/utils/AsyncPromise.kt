package eu.pkgsoftware.babybuddywidgets.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AsyncPromiseFailure(val value: Any) : Exception()

class AsyncPromise() {
    companion object {
        suspend inline fun <S, F> call(crossinline body: (promise: Promise<S, F>) -> Unit): S {
            return withContext(Dispatchers.Main) {
                suspendCoroutine<S> { continuation ->
                    val callbacks = object : Promise<S, F> {
                        override fun failed(f: F) {
                            continuation.resumeWithException(AsyncPromiseFailure(f as Any))
                        }

                        override fun succeeded(s: S) {
                            continuation.resume(s)
                        }
                    }
                    body.invoke(callbacks)
                }
            }
        }
    }
}

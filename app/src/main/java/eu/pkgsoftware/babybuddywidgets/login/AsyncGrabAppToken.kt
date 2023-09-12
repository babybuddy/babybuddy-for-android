package eu.pkgsoftware.babybuddywidgets.login

import kotlinx.coroutines.newSingleThreadContext
import java.net.URL

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.suspendCoroutine

class AsyncGrabAppToken(val url: URL) {
    private val grabAppToken = GrabAppToken(url)

    private suspend fun suspendExecution(r: Runnable) {
        suspendCoroutine<Unit> {
            var outerException: Exception? = null

            val t = object : Thread() {
                override fun run() {
                    try {

                    }
                    catch (e: Exception) {
                        outerException = e
                    }
                }
            }
            t.join()
        }
    }

    suspend fun login(username: String, password: String) {
        coroutineScope {
            launch(Dispatchers.Default) {
                delay(5000)
                grabAppToken.login(username, password)
            }.join()
        }
    }

    suspend fun getFromProfilePage() {
        coroutineScope {
            launch(Dispatchers.Default) {
                delay(5000)
                grabAppToken.login(username, password)
            }.join()
        }
    }
}

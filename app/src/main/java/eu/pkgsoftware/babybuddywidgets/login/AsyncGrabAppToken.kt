package eu.pkgsoftware.babybuddywidgets.login

import eu.pkgsoftware.babybuddywidgets.login.GrabAppToken.MissingPage
import kotlinx.coroutines.newSingleThreadContext
import java.net.URL

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AsyncGrabAppToken(val url: URL) {
    private val grabAppToken = GrabAppToken(url)

    suspend fun login(username: String, password: String) {
        coroutineScope {
            launch(Dispatchers.Unconfined) {
                grabAppToken.login(username, password)
            }.join()
        }
    }

    suspend fun fromProfilePage(): String? {
        val resultChannel = Channel<String?>()
        coroutineScope {
            launch(Dispatchers.Unconfined) {
                try {
                    val s = grabAppToken.getFromProfilePage()
                    resultChannel.send(s)
                }
                catch (e: MissingPage) {
                    resultChannel.send(null)
                }
                catch (e: IOException) {
                    e.printStackTrace()
                    resultChannel.send(null)
                }
            }.join()
        }
        return resultChannel.receive()
    }

    suspend fun parseFromSettingsPage(): String? {
        val resultChannel = Channel<String?>()
        coroutineScope {
            launch(Dispatchers.Unconfined) {
                try {
                    val s = grabAppToken.parseFromSettingsPage()
                    resultChannel.send(s)
                }
                catch (e: MissingPage) {
                    resultChannel.send(null)
                }
                catch (e: IOException) {
                    e.printStackTrace()
                    resultChannel.send(null)
                }
            }.join()
        }
        return resultChannel.receive()
    }
}

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
            launch(Dispatchers.Default) {
                grabAppToken.login(username, password)
            }.join()
        }
    }

    suspend fun fromProfilePage(): String? {
        var result: String? = null
        coroutineScope {
            launch(Dispatchers.Default) {
                try {
                    result = grabAppToken.getFromProfilePage()
                }
                catch (_: MissingPage) {
                }
            }.join()
        }
        return result
    }

    suspend fun parseFromSettingsPage(): String? {
        var result: String? = null
        coroutineScope {
            launch(Dispatchers.Default) {
                try {
                    result = grabAppToken.parseFromSettingsPage()
                }
                catch (_: MissingPage) {
                }
            }.join()
        }
        return result
    }
}

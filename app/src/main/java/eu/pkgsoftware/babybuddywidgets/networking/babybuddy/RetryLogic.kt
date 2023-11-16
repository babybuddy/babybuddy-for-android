package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import kotlinx.coroutines.delay
import java.io.IOException

val INITIAL_RETRY_INTERVAL = 200L
val EXPONENTIAL_BACKOFF_FACTOR_1000 = 1500L
val EXPONENTIAL_BACKOFF_LIMIT = 10000

interface ConnectingDialogInterface {
    fun showConnecting(currentTimeout: Long)
    fun hideConnecting()
}

suspend fun <T : Any> exponentialBackoff(dialog: ConnectingDialogInterface, block: suspend () -> T): T {
    val totalWaitTimeStart = System.currentTimeMillis()
    var currentRetryDelay = INITIAL_RETRY_INTERVAL
    var showingConnecting = false
    try {
        while (true) {
            try {
                return block.invoke()
            }
            catch (e: RequestCodeFailure) {
                if ((e.code >= 400) and (e.code < 500)) {
                    throw e
                }
            }
            catch (_: IOException) {}

            showingConnecting = true
            dialog.showConnecting(System.currentTimeMillis() - totalWaitTimeStart)

            delay(currentRetryDelay)
            currentRetryDelay = currentRetryDelay * EXPONENTIAL_BACKOFF_FACTOR_1000 / 1000L
            if (currentRetryDelay > EXPONENTIAL_BACKOFF_LIMIT) {
                currentRetryDelay = EXPONENTIAL_BACKOFF_LIMIT.toLong()
            }
        }
    }
    finally {
        if (showingConnecting) {
            dialog.hideConnecting()
        }
    }
}

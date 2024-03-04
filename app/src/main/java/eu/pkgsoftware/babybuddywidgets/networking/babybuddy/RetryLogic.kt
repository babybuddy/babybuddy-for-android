package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import kotlinx.coroutines.delay
import java.io.IOException

const val INITIAL_RETRY_INTERVAL = 200L
const val EXPONENTIAL_BACKOFF_FACTOR_1000 = 1500L
const val EXPONENTIAL_BACKOFF_LIMIT = 10000

interface ConnectingDialogInterface {
    fun interruptLoading(): Boolean
    fun showConnecting(currentTimeout: Long, error: Exception?)
    fun hideConnecting()
}

class InterruptedException : Exception("Exponential backoff interrupted")

suspend fun <T : Any> exponentialBackoff(conInterface: ConnectingDialogInterface, block: suspend () -> T): T {
    val totalWaitTimeStart = System.currentTimeMillis()
    var currentRetryDelay = INITIAL_RETRY_INTERVAL
    var showingConnecting = false
    try {
        while (true) {
            var error: Exception? = null
            try {
                return block.invoke()
            }
            catch (e: RequestCodeFailure) {
                if ((e.code >= 400) and (e.code < 500)) {
                    throw e
                }
                error = e
            }
            catch (e: IOException) {
                error = e
            }

            showingConnecting = true
            conInterface.showConnecting(
                System.currentTimeMillis() - totalWaitTimeStart,
                error,
            )
            if (conInterface.interruptLoading()) {
                throw InterruptedException()
            }

            delay(currentRetryDelay)
            currentRetryDelay = currentRetryDelay * EXPONENTIAL_BACKOFF_FACTOR_1000 / 1000L
            if (currentRetryDelay > EXPONENTIAL_BACKOFF_LIMIT) {
                currentRetryDelay = EXPONENTIAL_BACKOFF_LIMIT.toLong()
            }
        }
    }
    finally {
        if (showingConnecting) {
            conInterface.hideConnecting()
        }
    }
}

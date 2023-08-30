package eu.pkgsoftware.babybuddywidgets.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CancelParallel(val maxCalls: Int = 1) {
    private var parallelCalls = 0
    private val mutex = Mutex()

    suspend fun <R> cancelParallel(body: suspend () -> R): R? {
        mutex.withLock {
            if (parallelCalls >= maxCalls) {
                return null
            }
            parallelCalls++
        }

        try {
            return body.invoke()
        }
        finally {
            mutex.withLock {
                parallelCalls--
            }
        }
    }
}
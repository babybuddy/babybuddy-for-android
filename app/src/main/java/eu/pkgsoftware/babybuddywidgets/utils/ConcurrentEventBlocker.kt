package eu.pkgsoftware.babybuddywidgets.utils

class ConcurrentEventBlocker {
    private var internalBlockCounter = 0
    private val subscribed = mutableListOf<Promise<Unit, Exception>>()

    val isBlocked: Boolean
        get() = internalBlockCounter > 0

    suspend fun wait() {
        if (internalBlockCounter > 0) {
            AsyncPromise.call { promise ->
                subscribed.add(promise)
            }
        }
    }

    suspend fun register(body: suspend () -> Unit) {
        internalBlockCounter++
        try {
            body.invoke()
        }
        finally {
            internalBlockCounter--
            if (internalBlockCounter == 0) {
                subscribed.forEach { it.succeeded(Unit) }
                subscribed.clear()
            }
        }
    }
}
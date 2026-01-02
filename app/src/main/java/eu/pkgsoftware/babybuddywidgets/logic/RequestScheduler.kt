package eu.pkgsoftware.babybuddywidgets.logic

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

interface CallResult {
    fun isSuccess(): Boolean
    fun isConnectionFailure(): Boolean
    fun isFailure(): Boolean
}

interface ApplicationInterface {
    fun setDisconnected(reason: String, disconnected: Boolean)
    fun reportError(message: String, error: Exception?)
}

typealias SuspendingTask = suspend () -> CallResult

class RequestScheduler(val appinterface: ApplicationInterface) {
    private data class ScheduledEntry(
        val intervalMillis: Long,
        val task: SuspendingTask,
        val running: AtomicBoolean = AtomicBoolean(false),
        @Volatile var nextRun: Long = 0L,
        @Volatile var startTime: Long = 0L
    )

    private val entriesLock = Any()
    private val entries = mutableListOf<ScheduledEntry>()

    @Volatile
    private var enabled = false

    private var scope: CoroutineScope? = null
    private var schedulerJob: Job? = null

    // disconnected mode flag
    private val disconnected = AtomicBoolean(false)

    fun scheduleInterval(intervalMillis: Long, task: SuspendingTask) {
        val now = System.currentTimeMillis()
        val entry = ScheduledEntry(intervalMillis = intervalMillis, task = task, nextRun = now)
        synchronized(entriesLock) {
            entries.add(entry)
        }
    }

    fun startScheduler() {
        if (enabled) return
        enabled = true

        val newScope = CoroutineScope(Dispatchers.IO)
        scope = newScope
        schedulerJob = newScope.launch {
            mainLoop()
        }
    }

    fun stopScheduler() {
        enabled = false
        // cancel the running scope which cancels all launched task coroutines
        scope?.cancel()
        schedulerJob = null
        scope = null
        synchronized(entriesLock) {
            for (e in entries) e.running.set(false)
        }
    }

    private suspend fun runEntryOnce(entry: ScheduledEntry): CallResult {
        try {
            val result = try {
                entry.startTime = System.currentTimeMillis()
                entry.task()
            }
            catch (ex: Exception) {
                appinterface.reportError("Task exception", ex)

                // Treat exception as connection failure to trigger disconnected mode retry if appropriate.
                // But to be conservative, consider it a connection failure so disconnected-mode logic engages.
                object : CallResult {
                    override fun isSuccess(): Boolean = false
                    override fun isConnectionFailure(): Boolean = false
                    override fun isFailure(): Boolean = true
                }
            }

            return result
        } finally {
            entry.running.set(false)
        }
    }

    private suspend fun retryEntryWithBackoff(entry: ScheduledEntry, scopeCoroutine: CoroutineScope) {
        val MAX_BACKOFF = 5000L

        var backoff = 200L
        while (scopeCoroutine.isActive && enabled && disconnected.get()) {
            if (!entry.running.compareAndSet(false, true)) {
                delay(100)
                continue
            }

            val result = runEntryOnce(entry)
            handleTaskResult(result, entry)

            if (disconnected.get()) {
                delay(backoff)
                backoff = min(backoff * 3 / 2, MAX_BACKOFF)
            }
        }
    }

    private suspend fun CoroutineScope.mainLoop() {
        try {
            while (isActive && enabled) {
                val now = System.currentTimeMillis()
                val toRun = synchronized(entriesLock) {
                    entries.filter { it.nextRun <= now && !it.running.get() }
                }

                if (toRun.isEmpty()) {
                    // nothing to run, just delay a bit
                } else if (disconnected.get()) {
                    val entry = toRun.first()
                    retryEntryWithBackoff(entry, this)
                } else {
                    for (entry in toRun) {
                        if (entry.running.compareAndSet(false, true)) {
                            launch {
                                val result = runEntryOnce(entry)
                                handleTaskResult(result, entry)
                            }
                        }
                    }
                }
                delay(100)
            }
        }
        finally {
            // ensure running flags cleaned when scheduler stops
            synchronized(entriesLock) {
                for (e in entries) e.running.set(false)
            }
        }
    }

    private fun handleTaskResult(
        result: CallResult,
        entry: ScheduledEntry
    ) {
        if (result.isSuccess() || result.isFailure()) {
            entry.nextRun = entry.startTime + entry.intervalMillis

            if (result.isFailure()) {
                appinterface.reportError("Task reported failure", null)
            } else if (disconnected.getAndSet(false)) {
                try {
                    appinterface.setDisconnected("reconnect", false)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else if (result.isConnectionFailure()) {
            if (!disconnected.getAndSet(true)) {
                try {
                    appinterface.setDisconnected("reconnect", true)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
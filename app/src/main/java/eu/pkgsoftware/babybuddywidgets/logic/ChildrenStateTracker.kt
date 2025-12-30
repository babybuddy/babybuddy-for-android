package eu.pkgsoftware.babybuddywidgets.logic

import eu.pkgsoftware.babybuddywidgets.ActivityStore
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.Client
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Child
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChildrenList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

typealias ChildListener = (Array<Child>) -> Unit

class ChildrenStateTracker(val v2Client: Client, val activityStore: ActivityStore) {
    var children = emptyArray<Child>()

    private var isPolling = false
    private var listeners = mutableListOf<ChildListener>()

    init {
        activityStore.login<ChildrenList>("children-state-tracker") ?.let {
            children = it.children
        }
    }

    fun addChildListener(listener: ChildListener, triggerOnAdd: Boolean = true) {
        listeners.add(listener)
        if (triggerOnAdd) {
            listener(children)
        }
    }

    fun removeChildListener(listener: ChildListener) {
        listeners.remove(listener)
    }

    suspend fun refreshChildrenList() {
        val accumulatedChildrenList = mutableListOf<Child>();
        while (true) {
            val newChildren = v2Client.getEntries(
                Child::class,
                offset = accumulatedChildrenList.size,
                limit = 100
            )
            if (!isPolling) {
                return
            }
            if (newChildren.entries.isEmpty()) {
                break
            }
            accumulatedChildrenList.addAll(newChildren.entries)
            if (accumulatedChildrenList.size >= newChildren.totalCount) {
                break
            }
        }

        coroutineScope {
            launch(Dispatchers.Main) {
                if (!children.contentEquals(accumulatedChildrenList.toTypedArray())) {
                    val newChildData = ChildrenList(accumulatedChildrenList.toTypedArray())
                    children = newChildData.children
                    activityStore.login("children-state-tracker", children)

                    for (listener in listeners) {
                        listener(children)
                    }
                }
            }.join()
        }
    }

    fun startPolling() {
        if (isPolling) return

        isPolling = true

        CoroutineScope(Dispatchers.IO).launch {
            while (isPolling) {
                try {
                    refreshChildrenList()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                var totalDelay = 5000L
                while (totalDelay > 0 && isPolling) {
                    val delayChunk = 200L
                    kotlinx.coroutines.delay(delayChunk)
                    totalDelay -= delayChunk
                }
            }
        }
    }

    fun stopPolling() {
        if (!isPolling) return
    }
}
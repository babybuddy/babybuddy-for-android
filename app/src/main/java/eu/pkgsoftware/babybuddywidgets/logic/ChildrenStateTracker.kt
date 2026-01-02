package eu.pkgsoftware.babybuddywidgets.logic

import eu.pkgsoftware.babybuddywidgets.ActivityStore
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.Client
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Child
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChildrenList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

typealias ChildListener = (Array<Child>) -> Unit

class ChildrenStateTracker(
    val v2Client: Client, val activityStore: ActivityStore, val requestScheduler: RequestScheduler
) {
    var children = emptyArray<Child>()

    private var listeners = mutableListOf<ChildListener>()

    init {
        activityStore.login<ChildrenList>("children-state-tracker") ?.let {
            children = it.children
        }

        requestScheduler.scheduleInterval(5000) {
            refreshChildrenList()
            object : CallResult {
                override fun isSuccess(): Boolean = true
                override fun isConnectionFailure(): Boolean = false
                override fun isFailure(): Boolean = false
            }
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
}
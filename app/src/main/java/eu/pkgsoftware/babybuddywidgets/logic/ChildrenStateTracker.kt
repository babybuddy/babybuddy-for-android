package eu.pkgsoftware.babybuddywidgets.logic

import android.net.Network
import eu.pkgsoftware.babybuddywidgets.ActivityStore
import eu.pkgsoftware.babybuddywidgets.BaseFragmentDisconnectInterface
import eu.pkgsoftware.babybuddywidgets.debugging.GlobalDebugObject
import eu.pkgsoftware.babybuddywidgets.networking.CoordinatedDisconnectDialog
import eu.pkgsoftware.babybuddywidgets.networking.NetworkChangeListener
import eu.pkgsoftware.babybuddywidgets.networking.NetworkMonitor
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.Client
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.Child
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChildrenList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

typealias ChildListener = (Array<Child>) -> Unit

class ChildrenStateTracker(
    val v2Client: Client,
    val activityStore: ActivityStore,
    val requestScheduler: RequestScheduler,
    val disconnectInterface: BaseFragmentDisconnectInterface
) : NetworkChangeListener {
    var children = emptyArray<Child>()

    private var listeners = mutableListOf<ChildListener>()

    init {
        activityStore.login<ChildrenList>("children-state-tracker")?.let {
            children = it.children
        }

        v2Client.networkMonitor.addListener(this);
        requestScheduler.scheduleInterval(5000) {
            refreshChildrenList()
            object : CallResult {
                override fun isSuccess(): Boolean = true
                override fun isConnectionFailure(): Boolean = false
                override fun isFailure(): Boolean = false
            }
        }
    }

    override fun onNetworkChanged(newNetwork: Network?) {
        if (newNetwork == null) {
            disconnectInterface.setDisconnected(
                "No network connection available.",
                true
            )
        } else {
            requestScheduler.scheduleOnceNow {
                refreshChildrenList()
                object : CallResult {
                    override fun isSuccess(): Boolean = true
                    override fun isConnectionFailure(): Boolean = false
                    override fun isFailure(): Boolean = false
                }
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
        try {
            while (true) {
                val newChildren = v2Client.getEntries(
                    Child::class,
                    offset = accumulatedChildrenList.size,
                    limit = 100
                )
                disconnectInterface.setDisconnected("Refreshing children list...", false)
                if (newChildren.entries.isEmpty()) {
                    break
                }
                accumulatedChildrenList.addAll(newChildren.entries)
                accumulatedChildrenList.sortBy { it.id }
                if (accumulatedChildrenList.size >= newChildren.totalCount) {
                    break
                }
            }
        }
        catch (e: IOException) {
            GlobalDebugObject.log(
                "ChildrenStateTracker: Failed to refresh children list: ${e.message}"
            )
            disconnectInterface.setDisconnected("Failed to retrieve children list.", true)
            return
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

    fun destroy() {
        v2Client.networkMonitor.removeListener(this)
    }
}
package eu.pkgsoftware.babybuddywidgets.history

import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.VisibilityCheck
import eu.pkgsoftware.babybuddywidgets.debugging.GlobalDebugObject
import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListItem
import eu.pkgsoftware.babybuddywidgets.logic.EndAwareContinuousListIntegrator
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.ConnectingDialogInterface
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.InterruptedException
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.exponentialBackoff
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChangeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.SleepEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TummyTimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.FeedingEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.NoteEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.PumpingEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.classActivityName
import eu.pkgsoftware.babybuddywidgets.tutorial.Direction
import eu.pkgsoftware.babybuddywidgets.tutorial.Trackable
import kotlinx.coroutines.*
import kotlin.reflect.KClass

val IMPLEMENTED_EVENT_CLASSES = listOf(
    FeedingEntry::class,
    SleepEntry::class,
    TummyTimeEntry::class,
    ChangeEntry::class,
    NoteEntry::class,
    PumpingEntry::class,
)

interface ShowErrorPill {
    fun showErrorPill(entryType: String, exception: Exception?)
}

class ChildEventHistoryLoader(
    private val fragment: BaseFragment,
    private val container: LinearLayout,
    private val childId: Int,
    private val visibilityCheck: VisibilityCheck,
    private val progressBar: ProgressBar,
    private val errorPill: ShowErrorPill,
) {
    val HISTORY_ITEM_COUNT = 50
    val POLL_INTERVAL = 5000

    private val activityCollectionGate = IMPLEMENTED_EVENT_CLASSES.toMutableList()
    private val scope = fragment.mainActivity.scope

    private val timeEntryLookup = mutableMapOf<ContinuousListItem, TimeEntry>()
    private val listIntegrator = EndAwareContinuousListIntegrator()
    private val currentList = mutableListOf<TimelineEntry>()
    private val removedViews = mutableListOf<TimelineEntry>()

    private var updateUiJob: Job? = null
    private var fetchJob: Job? = null

    private val queryOffsets = mutableMapOf<KClass<*>, Int>()
    private var tutorialMessageAdded = false

    init {
        forceRefresh()
    }

    inner class BackoffConnectionInterface(val entryName: String) : ConnectingDialogInterface {
        private var retriesLeft = 3

        override fun interruptLoading(): Boolean {
            return retriesLeft <= 0
        }

        override fun showConnecting(currentTimeout: Long, error: Exception?) {
            retriesLeft--
            if (interruptLoading()) {
                errorPill.showErrorPill(entryName, error)
            }
        }

        override fun hideConnecting() {
        }
    }

    private fun startFetch() {
        val fetchJob = this.fetchJob
        if ((fetchJob == null) || (!fetchJob.isActive)) {
            this.fetchJob = scope.launch {
                IMPLEMENTED_EVENT_CLASSES.map {
                    async {
                        val activityName = classActivityName(it)
                        try {
                            val conInterface = BackoffConnectionInterface(activityName)
                            val r = exponentialBackoff(conInterface) {
                                fragment.mainActivity.client.v2client.getEntries(
                                    it,
                                    offset = queryOffsets.getOrDefault(it, 0),
                                    limit = HISTORY_ITEM_COUNT,
                                    childId = childId,
                                )
                            }
                            addTimelineItems(r.offset, r.totalCount, it, r.entries)
                        }
                        catch (e: InterruptedException) {
                            GlobalDebugObject.log("ChildEventHistoryLoader retrieval of ${it.simpleName} failed after retries")
                            addTimelineItems(0, 0, it, listOf())
                        }
                        catch (e: RequestCodeFailure) {
                            GlobalDebugObject.log("ChildEventHistoryLoader retrieval of ${it.simpleName} failed with code ${e.code}")
                            errorPill.showErrorPill(activityName, e)
                            addTimelineItems(0, 0, it, listOf())
                        }
                    }
                }.awaitAll()
            }
        }
    }

    private fun newTimelineEntry(e: TimeEntry?): TimelineEntry {
        val result = if (removedViews.size > 0) {
            removedViews.removeLast()
        } else {
            TimelineEntry(fragment, e)
        };
        result.entry = e
        result.setModifiedCallback {
            forceRefresh()
        }
        container.addView(result.view)
        currentList.add(result)
        return result
    }

    private fun timeEntryToContinuousListItem(e: TimeEntry): ContinuousListItem {
        val result = ContinuousListItem(
            -e.start.time,
            e.type,
            e.id.toString(),
        )
        timeEntryLookup[result] = e
        return result
    }

    private suspend fun addTimelineItems(
        offset: Int,
        totalCount: Int,
        type: KClass<*>,
        entries: List<TimeEntry>
    ) {
        activityCollectionGate.remove(type)

        // Put this in separate thread!
        listIntegrator.updateItemsWithCount(
            offset,
            totalCount,
            classActivityName(type),
            entries.map { timeEntryToContinuousListItem(it) }.toTypedArray()
        )

        queryOffsets[type] = listIntegrator.suggestClassQueryOffset(classActivityName(type))

        val updateUiJob = this.updateUiJob
        if ((updateUiJob == null) || (!updateUiJob.isActive)) {
            if (activityCollectionGate.isEmpty()) {
                this.updateUiJob = scope.launch {
                    updateUiJobImpl()
                }
            }
        }
    }

    private suspend fun updateUiJobImpl() {
        try {
            deferredUpdate()
        }
        finally {
            delay(POLL_INTERVAL.toLong())
            scope.launch {
                this@ChildEventHistoryLoader.updateUiJob?.join()
                startFetch()
            }
        }
    }

    private suspend fun deferredUpdate() {
        val items = listIntegrator.items
        var i = 0
        val visibleCount = listIntegrator.computeValidCount()

        for (it in items) {
            val entry = timeEntryLookup[it]
            val listItem: TimelineEntry =
                if (i < currentList.size) {
                    currentList[i]
                } else {
                    newTimelineEntry(entry)
                };
            if (it.dirty) {
                listItem.entry = null
            } else {
                listItem.entry = entry
            }
            listItem.view.visibility = if (i < visibleCount) {
                View.VISIBLE
            } else {
                View.GONE
            }
            i++
            if (i % 50 == 0) {
                delay(50)
            }
            attemptAddingLongClickTutorialMessage()
        }
        progressBar.visibility = if (visibleCount == items.size) {
            View.GONE
        } else {
            View.VISIBLE
        }
        while (currentList.size > items.size) {
            val removed = currentList.removeLast()
            if (removedViews.size < 128) {
                removedViews.add(removed)
            }
            container.removeView(removed.view)
        }
        attemptAddingLongClickTutorialMessage()
        delay(500)
    }

    private fun attemptAddingLongClickTutorialMessage() {
        if (tutorialMessageAdded) return
        if (container.childCount <= 0) return
        tutorialMessageAdded = true

        fragment.mainActivity.tutorialManagement.addItem(
            fragment.makeTutorialEntry(
                R.string.tutorial_long_click_notification,
                object : Trackable() {
                    override val orientation: Direction = Direction.DOWN
                    override val position: PointF? get() {
                        val r = Rect()
                        container.getGlobalVisibleRect(r)
                        println(r)
                        if (r.isEmpty) return null
                        return PointF((r.left + r.right) / 2f, r.top.toFloat())
                    }
                }
            )
        )
        fragment.mainActivity.tutorialManagement.updateArrows()
    }

    fun close() {
        updateUiJob?.cancel()
        fetchJob?.cancel()
        listIntegrator.clear()
        removedViews.clear()
        container.removeAllViews()
        timeEntryLookup.clear()
        queryOffsets.clear()
        tutorialMessageAdded = false
    }

    fun updateTop() {
        var i = 0
        listIntegrator.top = null
        for (item in currentList) {
            if (visibilityCheck.checkPartiallyVisible(item.view)) {
                listIntegrator.top = listIntegrator.items[i]
                break
            }
            i++
        }

        for (cls in IMPLEMENTED_EVENT_CLASSES) {
            queryOffsets[cls] = listIntegrator.suggestClassQueryOffset(classActivityName(cls))
        }
    }

    fun forceRefresh() {
        updateUiJob?.cancel("forceRefresh()")
        fetchJob?.cancel("forceRefresh()")
        activityCollectionGate.clear()
        activityCollectionGate.addAll(IMPLEMENTED_EVENT_CLASSES)
        startFetch()
    }
}
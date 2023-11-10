package eu.pkgsoftware.babybuddywidgets.history

import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.VisibilityCheck
import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListItem
import eu.pkgsoftware.babybuddywidgets.logic.EndAwareContinuousListIntegrator
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChangeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.SleepEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TummyTimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.FeedingEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.classActivityName
import kotlinx.coroutines.*
import kotlin.reflect.KClass

val IMPLEMENTED_EVENT_CLASSES = listOf(
    FeedingEntry::class,
    SleepEntry::class,
    TummyTimeEntry::class,
    ChangeEntry::class,
)

class ChildEventHistoryLoader(
    private val fragment: BaseFragment,
    private val container: LinearLayout,
    private val childId: Int,
    private val visibilityCheck: VisibilityCheck,
    private val progressBar: ProgressBar
) {
    val HISTORY_ITEM_COUNT = 50
    val POLL_INTERVAL = 5000

    private val activityCollectionGate = IMPLEMENTED_EVENT_CLASSES.toMutableList()
    private val scope = fragment.mainActivity.scope

    private val timeEntryLookup = mutableMapOf<ContinuousListItem, TimeEntry>()
    private val listIntegrator = EndAwareContinuousListIntegrator()
    private val currentList = mutableListOf<TimelineEntry>()
    private val removedViews = mutableListOf<TimelineEntry>()

    private var updateJob: Job? = null

    private val queryOffsets = mutableMapOf<KClass<*>, Int>()

    init {
        forceRefresh()
    }

    private fun startFetch() {
        scope.launch {
            IMPLEMENTED_EVENT_CLASSES.map {
                async {
                    // TODO: Add exception handling and show "connecting" error
                    val r = fragment.mainActivity.client.v2client.getEntries(
                        it,
                        offset = queryOffsets.getOrDefault(it, 0),
                        limit = HISTORY_ITEM_COUNT,
                        childId=childId,
                    )
                    addTimelineItems(r.offset, r.totalCount, it, r.entries as List<TimeEntry>)
                }
            }
        }
    }

    private fun newTimelineEntry(e: TimeEntry?): TimelineEntry {
        val result = if (removedViews.size > 0) {
            removedViews.removeLast()
        } else {
            TimelineEntry(fragment, e)
        };
        result.timeEntry = e
        result.setModifiedCallback {
            startFetch()
        }
        container.addView(result.view)
        currentList.add(result)
        return result
    }

    private fun timeEntryToContinuousListItem(e: TimeEntry): ContinuousListItem {
        val result = ContinuousListItem(
            -e.start.time,
            e.type,
            e.typeId.toString()
        )
        timeEntryLookup[result] = e
        return result
    }

    private suspend fun addTimelineItems(offset: Int, totalCount: Int, type: KClass<*>, entries: List<TimeEntry>) {
        activityCollectionGate.remove(type)

        // Put this in separate thread!
        listIntegrator.updateItemsWithCount(
            offset,
            totalCount,
            classActivityName(type),
            entries.map { timeEntryToContinuousListItem(it) }.toTypedArray()
        )

        queryOffsets[type] = listIntegrator.suggestClassQueryOffset(classActivityName(type))

        val updateJob = this.updateJob
        if ((updateJob == null) || (!updateJob.isActive)) {
            if (activityCollectionGate.isEmpty()) {
                this.updateJob = scope.launch {
                    updateJobImpl()
                }
            }
        }
    }

    private suspend fun updateJobImpl() {
        println("updateJobImpl ${childId}")
        try {
            deferredUpdate()
        }
        finally {
            delay(POLL_INTERVAL.toLong())
            scope.launch {
                println("updateJobImpl internal ${childId}")
                this@ChildEventHistoryLoader.updateJob?.join()
                startFetch()
            }
        }
        println("updateJobImpl exit ${childId}")
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
                listItem.timeEntry = null
            } else {
                listItem.timeEntry = entry
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

        delay(500)
    }

    fun close() {
        updateJob?.cancel()
        listIntegrator.clear()
        removedViews.clear()
        container.removeAllViews()
        timeEntryLookup.clear()
        queryOffsets.clear()
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
        updateJob?.cancel("forceRefresh()")
        activityCollectionGate.clear()
        activityCollectionGate.addAll(IMPLEMENTED_EVENT_CLASSES)
        startFetch()
    }
}
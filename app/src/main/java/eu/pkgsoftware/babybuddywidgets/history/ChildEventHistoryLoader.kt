package eu.pkgsoftware.babybuddywidgets.history

import android.widget.LinearLayout
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.VisibilityCheck
import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListIntegrator
import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListItem
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.EVENTS
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker.TimelineListener
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker.TimelineObserver
import java.util.*

class ChildEventHistoryLoader(
    private val fragment: BaseFragment,
    private val container: LinearLayout,
    private val childId: Int,
    private val visibilityCheck: VisibilityCheck
) {
    private var timelineObserver: TimelineObserver? = null
    private val timeEntryLookup = mutableMapOf<ContinuousListItem, TimeEntry>()
    private val listIntegrator = ContinuousListIntegrator()
    private val currentList = mutableListOf<TimelineEntry>()
    private val removedViews = mutableListOf<TimelineEntry>()

    fun createTimelineObserver(stateTracker: ChildrenStateTracker) {
        close()
        timelineObserver = stateTracker.TimelineObserver(childId, object : TimelineListener {
            override fun sleepRecordsObtained(offset: Int, entries: Array<TimeEntry>) {
                addTimelineItems(offset, ACTIVITIES.SLEEP, entries)
            }

            override fun tummyTimeRecordsObtained(offset: Int, entries: Array<TimeEntry>) {
                addTimelineItems(offset, ACTIVITIES.TUMMY_TIME, entries)
            }

            override fun feedingRecordsObtained(offset: Int, entries: Array<TimeEntry>) {
                addTimelineItems(offset, ACTIVITIES.FEEDING, entries)
            }

            override fun changeRecordsObtained(offset: Int, entries: Array<TimeEntry>) {
                addTimelineItems(offset, EVENTS.CHANGE, entries)
            }
        })
    }

    private fun newTimelineEntry(e: TimeEntry?): TimelineEntry {
        val result = if (removedViews.size > 0) {
            removedViews.removeLast()
        } else {
            TimelineEntry(fragment, e)
        };
        result.timeEntry = e
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
        timeEntryLookup.put(result, e)
        return result
    }

    private fun addTimelineItems(offset: Int, type: String, _entries: Array<TimeEntry>) {
        val to = timelineObserver ?: return

        System.out.println("new timeline entries: ${type}  o:${offset} c:${_entries.size}")

        listIntegrator.updateItems(
            offset,
            type,
            _entries.map { timeEntryToContinuousListItem(it) }.toTypedArray()
        )

        val newOffset = listIntegrator.suggestClassQueryOffset(type)
        to.queryOffsets[type] = newOffset
        if (newOffset != offset) {
            // TODO: Fix!
            //to.forceUpdate()
        }

        updateTimelineList()
    }

    private fun updateTimelineList() {
        val items = listIntegrator.items
        var i = 0

        items.forEach {
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
            i++
        }
        while (currentList.size > items.size) {
            val removed = currentList.removeLast()
            removedViews.add(removed)
            container.removeView(removed.view)
        }
    }

    fun close() {
        timelineObserver?.close()
        timelineObserver = null
        listIntegrator.clear()
        removedViews.clear()
        updateTimelineList()
        timeEntryLookup.clear()
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
        System.out.println("top-item updated: ${listIntegrator.top}")

        val to = timelineObserver ?: return

        for (clsName in ACTIVITIES.ALL) {
            to.queryOffsets[clsName] = listIntegrator.suggestClassQueryOffset(clsName)
            System.out.println("Suggested: ${clsName}  ${to.queryOffsets[clsName]}")
        }
        for (clsName in EVENTS.ALL) {
            to.queryOffsets[clsName] = listIntegrator.suggestClassQueryOffset(clsName)
            System.out.println("Suggested: ${clsName}  ${to.queryOffsets[clsName]}")
        }
    }
}
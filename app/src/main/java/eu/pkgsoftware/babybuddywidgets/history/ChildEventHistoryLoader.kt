package eu.pkgsoftware.babybuddywidgets.history

import android.view.ViewGroup
import android.widget.LinearLayout
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.VisibilityCheck
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
    private val timeEntries: MutableList<TimeEntry> = ArrayList(100)
    private val visualTimelineEntries: MutableList<TimelineEntry> = ArrayList(100)

    fun createTimelineObserver(stateTracker: ChildrenStateTracker) {
        close()
        timelineObserver = stateTracker.TimelineObserver(childId, object : TimelineListener {
            override fun sleepRecordsObtained(entries: Array<TimeEntry>) {
                addTimelineItems(ACTIVITIES.SLEEP, entries)
            }

            override fun tummyTimeRecordsObtained(entries: Array<TimeEntry>) {
                addTimelineItems(ACTIVITIES.TUMMY_TIME, entries)
            }

            override fun feedingRecordsObtained(entries: Array<TimeEntry>) {
                addTimelineItems(ACTIVITIES.FEEDING, entries)
            }

            override fun changeRecordsObtained(entries: Array<TimeEntry>) {
                addTimelineItems(EVENTS.CHANGE, entries)
            }
        })
    }

    private fun addTimelineItems(type: String, _entries: Array<TimeEntry>) {
        val entries = HashSet(Arrays.asList(*_entries))
        val newItems: MutableList<TimeEntry> = ArrayList(entries.size)
        val removedItems: MutableList<TimeEntry> = ArrayList(entries.size)
        for (e in timeEntries) {
            if (!entries.contains(e) && type == e.type) {
                removedItems.add(e)
            }
        }
        for (e in entries) {
            if (!timeEntries.contains(e)) {
                newItems.add(e)
            }
        }
        timeEntries.removeAll(removedItems)
        timeEntries.addAll(newItems)
        if (newItems.size + removedItems.size > 0) {
            updateTimelineList()
        }
    }

    private fun updateTimelineList() {
        while (visualTimelineEntries.size > timeEntries.size) {
            val v = visualTimelineEntries.removeAt(visualTimelineEntries.size - 1).view
            container.removeView(v)
        }
        for (i in visualTimelineEntries.indices) {
            visualTimelineEntries[i].timeEntry = timeEntries[i]
        }
        while (visualTimelineEntries.size < timeEntries.size) {
            val e = TimelineEntry(
                fragment,
                timeEntries[visualTimelineEntries.size]
            )
            visualTimelineEntries.add(e)
        }
        visualTimelineEntries.sortWith { a, b ->
            b.date.compareTo(
                a.date
            )
        };
        container.removeAllViews()
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, fragment.dpToPx(4f), 0, fragment.dpToPx(4f))
        for (e in visualTimelineEntries) {
            val v = e.view
            container.addView(v, params)
        }
    }

    fun close() {
        timelineObserver?.close()
        timelineObserver = null
        timeEntries.clear()
        updateTimelineList()
    }
}
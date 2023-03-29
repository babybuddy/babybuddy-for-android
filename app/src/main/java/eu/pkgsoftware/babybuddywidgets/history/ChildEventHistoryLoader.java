package eu.pkgsoftware.babybuddywidgets.history;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.phrase.Phrase;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import eu.pkgsoftware.babybuddywidgets.BaseFragment;
import eu.pkgsoftware.babybuddywidgets.R;
import eu.pkgsoftware.babybuddywidgets.VisibilityCheck;
import eu.pkgsoftware.babybuddywidgets.databinding.TimelineItemBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;

public class ChildEventHistoryLoader {
    private final int childId;
    private final BaseFragment fragment;
    private final LinearLayout container;
    private final VisibilityCheck visibilityCheck;
    private ChildrenStateTracker.TimelineObserver timelineObserver = null;

    private final List<BabyBuddyClient.TimeEntry> timeEntries = new ArrayList<>(100);
    private final List<TimelineEntry> visualTimelineEntries = new ArrayList<>(100);

    public ChildEventHistoryLoader(
        BaseFragment fragment,
        LinearLayout ll,
        int childId,
        VisibilityCheck visibilityCheck
    ) {
        this.fragment = fragment;
        this.container = ll;
        this.childId = childId;
        this.visibilityCheck = visibilityCheck;
    }

    public void createTimelineObserver(ChildrenStateTracker stateTracker) {
        close();

        this.timelineObserver = stateTracker.new TimelineObserver(childId, new ChildrenStateTracker.TimelineListener() {
            @Override
            public void sleepRecordsObtained(BabyBuddyClient.TimeEntry[] entries) {
                addTimelineItems("sleep", entries);
            }

            @Override
            public void tummyTimeRecordsObtained(BabyBuddyClient.TimeEntry[] entries) {
                addTimelineItems("tummy-time", entries);
            }

            @Override
            public void feedingRecordsObtained(BabyBuddyClient.TimeEntry[] entries) {
                addTimelineItems("feeding", entries);
            }

            @Override
            public void changeRecordsObtained(BabyBuddyClient.TimeEntry[] entries) {
                addTimelineItems("change", entries);
            }
        });
    }

    private void addTimelineItems(String type, BabyBuddyClient.TimeEntry[] _entries) {
        HashSet<BabyBuddyClient.TimeEntry> entries = new HashSet<>(Arrays.asList(_entries));

        List<BabyBuddyClient.TimeEntry> newItems = new ArrayList<>(entries.size());
        List<BabyBuddyClient.TimeEntry> removedItems = new ArrayList<>(entries.size());

        for (BabyBuddyClient.TimeEntry e : timeEntries) {
            if ((!entries.contains(e)) && (type.equals(e.type))) {
                removedItems.add(e);
            }
        }
        for (BabyBuddyClient.TimeEntry e : entries) {
            if (!timeEntries.contains(e)) {
                newItems.add(e);
            }
        }

        timeEntries.removeAll(removedItems);
        timeEntries.addAll(newItems);

        if ((newItems.size() + removedItems.size()) > 0) {
            updateTimelineList();
        }
    }

    private void updateTimelineList() {
        while (visualTimelineEntries.size() > timeEntries.size()) {
            View v = visualTimelineEntries.remove(visualTimelineEntries.size() - 1).getView();
            container.removeView(v);
        }

        for (int i = 0; i < visualTimelineEntries.size(); i++) {
            visualTimelineEntries.get(i).setTimeEntry(timeEntries.get(i));
        }
        while (visualTimelineEntries.size() < timeEntries.size()) {
            TimelineEntry e = new TimelineEntry(
                fragment,
                timeEntries.get(visualTimelineEntries.size())
            );
            visualTimelineEntries.add(e);
        }
        visualTimelineEntries.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        container.removeAllViews();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, fragment.dpToPx(4), 0, fragment.dpToPx(4));

        for (TimelineEntry e : visualTimelineEntries) {
            View v = e.getView();
            container.addView(v, params);
        }
    }

    public void close() {
        if (timelineObserver != null) {
            timelineObserver.close();
            timelineObserver = null;
        }
        timeEntries.clear();
        updateTimelineList();
    }
}

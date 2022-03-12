package eu.pkgsoftware.babybuddywidgets;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.phrase.Phrase;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import eu.pkgsoftware.babybuddywidgets.databinding.TimelineItemBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;

public class ChildEventHistoryLoader {
    private static class TimelineEntry {
        private BabyBuddyClient.TimeEntry entry;
        private TextView text;

        private TimelineItemBinding binding;

        private void hideAllSubviews() {
            for (int i = 0; i < binding.getRoot().getChildCount(); i++) {
                View c = binding.getRoot().getChildAt(i);
                c.setVisibility(View.GONE);
            }
        }

        private void configureDefaultView() {
            //hideAllSubviews();
            binding.getRoot().getChildAt(0).setVisibility(View.VISIBLE);

            String message = Phrase.from("{type}\n{start_date}  {start_time} - {end_time}")
                .put("type", entry.type)
                .putOptional("start_date", DATE_FORMAT.format(entry.start))
                .putOptional("start_time", TIME_FORMAT.format(entry.start))
                .putOptional("end_date", DATE_FORMAT.format(entry.end))
                .putOptional("end_time", TIME_FORMAT.format(entry.end))
                .format().toString();

            binding.defaultContent.setText(message);
        }
        
        public TimelineEntry(BaseFragment fragment, BabyBuddyClient.TimeEntry entry) {
            binding = TimelineItemBinding.inflate(fragment.getMainActivity().getLayoutInflater());

            text = new TextView(fragment.getContext());
            text.setTextAppearance(android.R.style.TextAppearance_DeviceDefault);
            setTimeEntry(entry);
        }

        public void setTimeEntry(BabyBuddyClient.TimeEntry entry) {
            this.entry = entry;

            configureDefaultView();
        }

        public BabyBuddyClient.TimeEntry getTimeEntry() {
            return entry;
        }

        public View getView() {
            return binding.getRoot();
        }

        public Date getDate() {
            return entry.end;
        }
    }

    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
    private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);

    private int childId;
    private BaseFragment fragment;
    private LinearLayout container;
    private ChildrenStateTracker.TimelineObserver timelineObserver = null;

    private List<BabyBuddyClient.TimeEntry> timeEntries = new ArrayList<>(100);
    private List<TimelineEntry> visualTimelineEntries = new ArrayList<>(100);

    public ChildEventHistoryLoader(BaseFragment fragment, LinearLayout ll, int childId) {
        this.fragment = fragment;
        this.container = ll;
        this.childId = childId;
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
            if ((!entries.contains(e)) && (e.type.equals(e))) {
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

        if ((newItems.size() + removedItems.size()) > 0 )
        updateTimelineList();
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

package eu.pkgsoftware.babybuddywidgets;

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

import eu.pkgsoftware.babybuddywidgets.databinding.TimelineItemBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.ChildrenStateTracker;

public class ChildEventHistoryLoader {
    private static class TimelineEntry {
        private BaseFragment fragment;
        private TimelineItemBinding binding;

        private BabyBuddyClient.TimeEntry entry = null;

        private void hideAllSubviews() {
            for (int i = 0; i < binding.viewGroup.getChildCount(); i++) {
                View c = binding.viewGroup.getChildAt(i);
                c.setVisibility(View.GONE);
            }
        }

        private Phrase defaultPhraseFields(Phrase phrase) {
            return phrase
                .putOptional("type", entry.type)
                .putOptional("start_date", DATE_FORMAT.format(entry.start))
                .putOptional("start_time", TIME_FORMAT.format(entry.start))
                .putOptional("end_date", DATE_FORMAT.format(entry.end))
                .putOptional("end_time", TIME_FORMAT.format(entry.end))
                .putOptional("notes", entry.notes.trim());
        }

        private void configureDefaultView() {
            hideAllSubviews();
            binding.viewGroup.getChildAt(0).setVisibility(View.VISIBLE);

            String message = defaultPhraseFields(
                Phrase.from("{type}\n{start_date}  {start_time} - {end_time}")
            ).format().toString();

            binding.defaultContent.setText(message);
        }

        private void configureTummyTime() {
            hideAllSubviews();
            binding.tummyTimeView.setVisibility(View.VISIBLE);

            String message = defaultPhraseFields(
                Phrase.from("{start_date}  {start_time} - {end_time}\n{notes}")
            ).format().toString().trim();

            binding.tummytimeMilestoneText.setText(message);
        }

        private void configureChange() {
            hideAllSubviews();
            binding.diaperView.setVisibility(View.VISIBLE);

            BabyBuddyClient.ChangeEntry change = (BabyBuddyClient.ChangeEntry) entry;
            binding.diaperWetImage.setVisibility(change.wet ? View.VISIBLE : View.GONE);
            binding.diaperSolidImage.setVisibility(change.solid ? View.VISIBLE : View.GONE);

            String message = defaultPhraseFields(
                Phrase.from("{start_date}  {start_time}\n{notes}")
            ).format().toString().trim();

            binding.diaperText.setText(message.trim());
        }

        private void configureSleep() {
            hideAllSubviews();
            binding.sleepView.setVisibility(View.VISIBLE);

            String message = defaultPhraseFields(
                Phrase.from("{start_date}  {start_time} - {end_time}\n{notes}")
            ).format().toString().trim();

            binding.sleepText.setText(message.trim());
        }

        private void configureFeeding() {
            hideAllSubviews();
            binding.feedingView.setVisibility(View.VISIBLE);

            BabyBuddyClient.FeedingEntry feeding = (BabyBuddyClient.FeedingEntry) entry;

            binding.feedingBreastImage.setVisibility(View.GONE);
            binding.feedingBottleImage.setVisibility(View.GONE);
            binding.solidFoodImage.setVisibility(View.GONE);

            switch (feeding.feedingType) {
                case BREAST_MILK:
                    binding.feedingBreastImage.setVisibility(View.VISIBLE);
                    break;
                case FORTIFIED_BREAST_MILK:
                case FORMULA:
                    binding.feedingBottleImage.setVisibility(View.VISIBLE);
                    break;
                case SOLID_FOOD:
                    binding.solidFoodImage.setVisibility(View.VISIBLE);
                    break;
                default:
                    binding.solidFoodImage.setVisibility(View.VISIBLE);
            }

            String message = defaultPhraseFields(
                Phrase.from("{start_date}  {start_time} - {end_time}\n{notes}")
            ).format().toString().trim();

            binding.feedingText.setText(message.trim());
        }

        private boolean longClick() {
            if (entry != null) {
                BabyBuddyClient client = fragment.getMainActivity().getClient();
                try {
                    fragment.showUrlInBrowser(client.pathToUrl(entry.getUserPath()).toString());
                    return true;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return false;
        }

        private void removeClick() {
            if (entry == null) {
                return;
            }

            fragment.showQuestion(
                true,
                "Delete entry",
                defaultPhraseFields(
                    Phrase.from(
                        "Are you sure you want to delete the {type} entry " +
                        "from {start_date} {start_time} - {end_time}?"
                    )
                ).format().toString().trim(),
                "Delete",
                "Cancel",
                b -> {
                    if (!b) {
                        return;
                    }
                    BabyBuddyClient client = fragment.getMainActivity().getClient();
                    client.removeTimelineEntry(entry, new BabyBuddyClient.RequestCallback<Boolean>() {
                        @Override
                        public void error(Exception error) {
                        }

                        @Override
                        public void response(Boolean response) {
                        }
                    });
                }
            );
        }

        public TimelineEntry(BaseFragment fragment, BabyBuddyClient.TimeEntry entry) {
            this.fragment = fragment;

            binding = TimelineItemBinding.inflate(fragment.getMainActivity().getLayoutInflater());
            setTimeEntry(entry);

            binding.getRoot().setOnLongClickListener((v) -> longClick());
            binding.removeButton.setOnClickListener((v) -> removeClick());
        }

        public void setTimeEntry(BabyBuddyClient.TimeEntry entry) {
            this.entry = entry;

            if ("tummy-time".equals(entry.type)) {
                configureTummyTime();
            } else if ("change".equals(entry.type)) {
                configureChange();
            } else if ("sleep".equals(entry.type)) {
                configureSleep();
            } else if ("feeding".equals(entry.type)) {
                configureFeeding();
            } else {
                configureDefaultView();
            }
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

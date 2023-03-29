package eu.pkgsoftware.babybuddywidgets.history;

import android.view.View;

import com.squareup.phrase.Phrase;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.Date;

import eu.pkgsoftware.babybuddywidgets.BaseFragment;
import eu.pkgsoftware.babybuddywidgets.R;
import eu.pkgsoftware.babybuddywidgets.databinding.TimelineItemBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;

public class TimelineEntry {
    public static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
    public static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);

    private final BaseFragment fragment;
    private final TimelineItemBinding binding;

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
            fragment.getResources().getString(R.string.history_delete_title),
            defaultPhraseFields(
                Phrase.from(fragment.getMainActivity(), R.string.history_delete_question)
            ).format().toString().trim(),
            fragment.getResources().getString(R.string.history_delete_question_delete_button),
            fragment.getResources().getString(R.string.history_delete_question_cancel_button),
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
                        setTimeEntry(null);
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

        if (entry == null) {
            binding.getRoot().setVisibility(View.GONE);
        } else {
            binding.getRoot().setVisibility(View.VISIBLE);

            if (BabyBuddyClient.ACTIVITIES.TUMMY_TIME.equals(entry.type)) {
                configureTummyTime();
            } else if (BabyBuddyClient.EVENTS.CHANGE.equals(entry.type)) {
                configureChange();
            } else if (BabyBuddyClient.ACTIVITIES.SLEEP.equals(entry.type)) {
                configureSleep();
            } else if (BabyBuddyClient.ACTIVITIES.FEEDING.equals(entry.type)) {
                configureFeeding();
            } else {
                configureDefaultView();
            }
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

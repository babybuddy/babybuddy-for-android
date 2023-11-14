package eu.pkgsoftware.babybuddywidgets.history;

import android.view.View;

import com.squareup.phrase.Phrase;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

import eu.pkgsoftware.babybuddywidgets.BaseFragment;
import eu.pkgsoftware.babybuddywidgets.Constants;
import eu.pkgsoftware.babybuddywidgets.R;
import eu.pkgsoftware.babybuddywidgets.databinding.TimelineItemBinding;
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient;
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChangeEntry;
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.FeedingEntry;
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry;

public class TimelineEntry {
    public static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
    public static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);

    private final BaseFragment fragment;
    private final TimelineItemBinding binding;

    private TimeEntry entry = null;

    private Runnable modifiedCallback = null;

    private void hideAllSubviews() {
        for (int i = 0; i < binding.viewGroup.getChildCount(); i++) {
            View c = binding.viewGroup.getChildAt(i);
            c.setVisibility(View.GONE);
        }
    }

    private Phrase defaultPhraseFields(Phrase phrase) {
        return phrase
            .putOptional("type", entry.getType())
            .putOptional("start_date", DATE_FORMAT.format(entry.getStart()))
            .putOptional("start_time", TIME_FORMAT.format(entry.getStart()))
            .putOptional("end_date", DATE_FORMAT.format(entry.getEnd()))
            .putOptional("end_time", TIME_FORMAT.format(entry.getEnd()))
            .putOptional("notes", entry.getNotes().trim());
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

        ChangeEntry change = (ChangeEntry) entry;
        binding.diaperWetImage.setVisibility(change.getWet() ? View.VISIBLE : View.GONE);
        binding.diaperSolidImage.setVisibility(change.getSolid() ? View.VISIBLE : View.GONE);

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

    private void configureNote() {
        hideAllSubviews();
        binding.noteTimeView.setVisibility(View.VISIBLE);

        String message = defaultPhraseFields(
            Phrase.from("{start_date}  {start_time}\n{notes}")
        ).format().toString().trim();

        binding.noteTimeEntryText.setText(message.trim());
    }

    private void configurePumping() {
        hideAllSubviews();
        binding.pumpingTimeView.setVisibility(View.VISIBLE);

        String message = defaultPhraseFields(
            Phrase.from("{start_date}  {start_time} - {end_time}\n{notes}")
        ).format().toString().trim();

        binding.pumpingTimeNotes.setText(message.trim());
    }

    private void configureFeeding() {
        hideAllSubviews();
        binding.feedingView.setVisibility(View.VISIBLE);

        FeedingEntry feeding = (FeedingEntry) entry;

        binding.feedingBreastImage.setVisibility(View.GONE);
        binding.feedingBreastLeftImage.setVisibility(View.GONE);
        binding.feedingBreastRightImage.setVisibility(View.GONE);
        binding.feedingBottleImage.setVisibility(View.GONE);
        binding.solidFoodImage.setVisibility(View.GONE);

        switch (Constants.FeedingTypeEnum.byPostName(feeding.getFeedingType())) {
            case BREAST_MILK:
                final Constants.FeedingMethodEnum feedingMethod =
                    Constants.FeedingMethodEnum.byPostName(feeding.getFeedingMethod());
                if (feedingMethod.value == 1) {
                    binding.feedingBreastLeftImage.setVisibility(View.VISIBLE);
                    break;
                } else if (feedingMethod.value == 2) {
                    binding.feedingBreastRightImage.setVisibility(View.VISIBLE);
                    break;
                } else if (feedingMethod.value == 3) {
                    binding.feedingBreastImage.setVisibility(View.VISIBLE);
                    break;
                }
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
                fragment.showUrlInBrowser(client.v2client.entryUserPath(entry).toString());
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
        final TimeEntry thisEntry = entry;

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
                /* TODO  client.removeTimelineEntry(thisEntry, new BabyBuddyClient.RequestCallback<Boolean>() {
                    @Override
                    public void error(@NotNull Exception error) {
                    }

                    @Override
                    public void response(Boolean response) {
                        setTimeEntry(null);
                        if (modifiedCallback != null) {
                            modifiedCallback.run();
                        }
                    }
                });*/
            }
        );
    }

    public TimelineEntry(BaseFragment fragment, TimeEntry entry) {
        this.fragment = fragment;

        binding = TimelineItemBinding.inflate(fragment.getMainActivity().getLayoutInflater());
        setTimeEntry(entry);

        binding.getRoot().setOnLongClickListener((v) -> longClick());
        binding.removeButton.setOnClickListener((v) -> removeClick());
    }

    public void setTimeEntry(TimeEntry entry) {
        if (Objects.equals(this.entry, entry)) {
            return;
        }
        this.entry = entry;

        if (entry == null) {
            binding.getRoot().setVisibility(View.INVISIBLE);
        } else {
            binding.getRoot().setVisibility(View.VISIBLE);

            if (BabyBuddyClient.ACTIVITIES.TUMMY_TIME.equals(entry.getType())) {
                configureTummyTime();
            } else if (BabyBuddyClient.EVENTS.CHANGE.equals(entry.getType())) {
                configureChange();
            } else if (BabyBuddyClient.ACTIVITIES.SLEEP.equals(entry.getType())) {
                configureSleep();
            } else if (BabyBuddyClient.ACTIVITIES.FEEDING.equals(entry.getType())) {
                configureFeeding();
            } else if (BabyBuddyClient.EVENTS.NOTE.equals(entry.getType())) {
                configureNote();
            } else if (BabyBuddyClient.ACTIVITIES.PUMPING.equals(entry.getType())) {
                configurePumping();
            } else {
                configureDefaultView();
            }
        }
    }

    public TimeEntry getTimeEntry() {
        return entry;
    }

    public View getView() {
        return binding.getRoot();
    }

    public Date getDate() {
        return entry.getEnd();
    }

    public void setModifiedCallback(Runnable r) {
        modifiedCallback = r;
    }
}

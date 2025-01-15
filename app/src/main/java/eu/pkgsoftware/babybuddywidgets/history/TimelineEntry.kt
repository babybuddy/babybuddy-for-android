package eu.pkgsoftware.babybuddywidgets.history

import android.view.MotionEvent
import android.view.View
import com.squareup.phrase.Phrase
import eu.pkgsoftware.babybuddywidgets.BaseFragment
import eu.pkgsoftware.babybuddywidgets.Constants
import eu.pkgsoftware.babybuddywidgets.Constants.FeedingMethodEnum
import eu.pkgsoftware.babybuddywidgets.Constants.FeedingTypeEnum
import eu.pkgsoftware.babybuddywidgets.DialogCallback
import eu.pkgsoftware.babybuddywidgets.R
import eu.pkgsoftware.babybuddywidgets.databinding.TimelineItemBinding
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import eu.pkgsoftware.babybuddywidgets.networking.RequestCodeFailure
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChangeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.FeedingEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.serverTimeToClientTime
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.MalformedURLException
import java.text.DateFormat


class TimelineEntry(private val fragment: BaseFragment, private var _entry: TimeEntry?) {
    companion object {
        val DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT)
        val TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT)
    }

    private val binding = TimelineItemBinding.inflate(fragment.mainActivity.layoutInflater)
    private var modifiedCallback: Runnable? = null
    private fun hideAllSubviews() {
        for (i in 0 until binding.viewGroup.childCount) {
            val c = binding.viewGroup.getChildAt(i)
            c.visibility = View.GONE
        }
    }

    var entry: TimeEntry?
        get() = _entry
        set(v) {
            if (v == _entry) {
                return
            }
            _entry = v
            updateUi()
        }

    val view: View
        get() = binding.root

    private fun updateUi() {
        val entry = _entry
        if (entry == null) {
            binding.root.visibility = View.INVISIBLE
        } else {
            binding.root.visibility = View.VISIBLE
            if (BabyBuddyClient.ACTIVITIES.TUMMY_TIME == entry.appType) {
                configureTummyTime()
            } else if (BabyBuddyClient.EVENTS.CHANGE == entry.appType) {
                configureChange()
            } else if (BabyBuddyClient.ACTIVITIES.SLEEP == entry.appType) {
                configureSleep()
            } else if (BabyBuddyClient.ACTIVITIES.FEEDING == entry.appType) {
                configureFeeding()
            } else if (BabyBuddyClient.EVENTS.NOTE == entry.appType) {
                configureNote()
            } else if (BabyBuddyClient.ACTIVITIES.PUMPING == entry.appType) {
                configurePumping()
            } else {
                configureDefaultView()
            }
        }
    }

    init {
        binding.root.setOnLongClickListener { v: View? -> longClick() }
        binding.root.setOnTouchListener { v, event ->
            longClickStartStopHandler(v, event)
            false
        }
        binding.removeButton.setOnClickListener { v: View? -> removeClick() }
        updateUi()
    }

    private fun defaultPhraseFields(phrase: Phrase): Phrase {
        val local_start_time = serverTimeToClientTime(entry!!.start)
        val local_end_time = serverTimeToClientTime(entry!!.end)

        val start_time = TIME_FORMAT.format(local_start_time)
        val end_time = TIME_FORMAT.format(local_end_time)
        val opt_time_range = if (start_time == end_time) start_time else "$start_time - $end_time"

        return phrase
            .putOptional("type", entry!!.appType)
            .putOptional("start_date", DATE_FORMAT.format(local_start_time))
            .putOptional("start_time", TIME_FORMAT.format(local_start_time))
            .putOptional("end_date", DATE_FORMAT.format(local_end_time))
            .putOptional("end_time", TIME_FORMAT.format(local_end_time))
            .putOptional("opt_time_range", opt_time_range)
            .putOptional("notes", entry!!.notes.trim { it <= ' ' })
    }

    private fun configureDefaultView() {
        hideAllSubviews()
        binding.viewGroup.getChildAt(0).visibility = View.VISIBLE
        val message = defaultPhraseFields(
            Phrase.from("{type}\n{start_date}  {opt_time_range}")
        ).format().toString()
        binding.defaultContent.text = message
    }

    private fun configureTummyTime() {
        hideAllSubviews()
        binding.tummyTimeView.visibility = View.VISIBLE
        val message = defaultPhraseFields(
            Phrase.from("{start_date}  {opt_time_range}\n{notes}")
        ).format().toString().trim { it <= ' ' }
        binding.tummytimeMilestoneText.text = message
    }

    private fun configureChange() {
        hideAllSubviews()
        var amountString = ""

        binding.diaperView.visibility = View.VISIBLE
        (entry as ChangeEntry?)?.let { change ->
            binding.diaperWetImage.visibility =
                if (change.wet) View.VISIBLE else View.GONE
            binding.diaperSolidImage.visibility = if (change.solid) View.VISIBLE else View.GONE

            if (change.color.isNotEmpty()) {
                binding.diaperColorPreview.visibility = View.VISIBLE

                val colorEnumValue = Constants.SolidDiaperColorEnum.byPostName(change.color)
                fragment.resources.getColor(colorEnumValue.colorResId, null).let { color ->
                    binding.diaperColorPreview.setBackgroundColor(color)
                }
            } else {
                binding.diaperColorPreview.visibility = View.GONE
            }

            if (change.amount != null) {
                amountString =
                    Phrase.from(fragment.resources, R.string.diaper_amount_timeline_pattern)
                        .put("amount", String.format("%.3f", change.amount))
                    .format().toString()
                amountString += "\n"
            }
        }
        val message = defaultPhraseFields(
            Phrase.from("{start_date}  {start_time}\n{amount}{notes}")
        ).put("amount", amountString).format().toString().trim { it <= ' ' }
        binding.diaperText.text = message.trim { it <= ' ' }
    }

    private fun configureSleep() {
        hideAllSubviews()
        binding.sleepView.visibility = View.VISIBLE
        val message = defaultPhraseFields(
            Phrase.from("{start_date}  {opt_time_range}\n{notes}")
        ).format().toString().trim { it <= ' ' }
        binding.sleepText.text = message.trim { it <= ' ' }
    }

    private fun configureNote() {
        hideAllSubviews()
        binding.noteTimeView.visibility = View.VISIBLE
        val message = defaultPhraseFields(
            Phrase.from("{start_date}  {start_time}\n{notes}")
        ).format().toString().trim { it <= ' ' }
        binding.noteTimeEntryText.text = message.trim { it <= ' ' }
    }

    private fun configurePumping() {
        hideAllSubviews()
        binding.pumpingTimeView.visibility = View.VISIBLE
        val message = defaultPhraseFields(
            Phrase.from("{start_date}  {opt_time_range}\n{notes}")
        ).format().toString().trim { it <= ' ' }
        binding.pumpingTimeNotes.text = message.trim { it <= ' ' }
    }

    private fun configureFeeding() {
        hideAllSubviews()
        binding.feedingView.visibility = View.VISIBLE
        val feeding = entry!! as FeedingEntry
        binding.feedingBreastImage.visibility = View.GONE
        binding.feedingBreastLeftImage.visibility = View.GONE
        binding.feedingBreastRightImage.visibility = View.GONE
        binding.feedingBottleImage.visibility = View.GONE
        binding.solidFoodImage.visibility = View.GONE
        when (FeedingTypeEnum.byPostName(feeding.feedingType)) {
            FeedingTypeEnum.BREAST_MILK -> {
                val feedingMethod = FeedingMethodEnum.byPostName(
                    feeding.feedingMethod
                )
                if (feedingMethod.value == 1) {
                    binding.feedingBreastLeftImage.visibility = View.VISIBLE
                } else if (feedingMethod.value == 2) {
                    binding.feedingBreastRightImage.visibility = View.VISIBLE
                } else if (feedingMethod.value == 3) {
                    binding.feedingBreastImage.visibility = View.VISIBLE
                } else {
                    binding.feedingBottleImage.visibility = View.VISIBLE
                }
            }

            FeedingTypeEnum.FORTIFIED_BREAST_MILK, FeedingTypeEnum.FORMULA -> binding.feedingBottleImage.visibility =
                View.VISIBLE

            FeedingTypeEnum.SOLID_FOOD -> binding.solidFoodImage.visibility = View.VISIBLE
            else -> binding.solidFoodImage.visibility = View.VISIBLE
        }
        val message = defaultPhraseFields(
            Phrase.from("{start_date}  {opt_time_range}\n{notes}")
        ).format().toString().trim { it <= ' ' }
        binding.feedingText.text = message.trim { it <= ' ' }
    }

    private fun longClickStartStopHandler(v: View, event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            binding.longclickBubble.startGrow()
        } else if (event.action in listOf(
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE,
            )
        ) {
            binding.longclickBubble.stopGrow()
        }
    }

    private fun longClick(): Boolean {
        binding.longclickBubble.stopGrow()
        val thisEntry = entry ?: return false
        val client = fragment.mainActivity.client.v2client
        return try {
            fragment.showUrlInBrowser(client.entryUserPath(thisEntry).toString())
            true
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            false
        }
    }

    private fun removeClick() {
        val thisEntry: TimeEntry = entry ?: return
        fragment.showQuestion(
            true,
            fragment.resources.getString(R.string.history_delete_title),
            defaultPhraseFields(
                Phrase.from(fragment.mainActivity, R.string.history_delete_question)
            ).format().toString().trim { it <= ' ' },
            fragment.resources.getString(R.string.history_delete_question_delete_button),
            fragment.resources.getString(R.string.history_delete_question_cancel_button),
            object : DialogCallback {
                override fun call(b: Boolean) {
                    if (!b) {
                        return
                    }
                    val client = fragment.mainActivity.client
                    fragment.mainActivity.scope.launch {
                        try {
                            client.v2client.deleteEntry(thisEntry)
                            this@TimelineEntry.entry = null
                            modifiedCallback?.run()
                        } catch (e: RequestCodeFailure) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }                }
            }
        )
    }

    fun setModifiedCallback(r: Runnable?) {
        modifiedCallback = r
    }
}
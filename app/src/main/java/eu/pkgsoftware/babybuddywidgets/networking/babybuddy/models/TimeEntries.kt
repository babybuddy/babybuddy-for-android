package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.EVENTS
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.DateTimeDeserializer
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.DateOnlyDeserializer
import java.util.Date

annotation class ActivityNameAnnotation(val name: String)

interface TimeEntry {
    val type: String
    val typeId: Int
    val id: Int
    val childId: Int
    val start: Date
    val end: Date?
    val notes: String
}


@JsonIgnoreProperties(ignoreUnknown = true)
@ActivityNameAnnotation(ACTIVITIES.SLEEP)
data class SleepEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("start", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val start: Date,
    @JsonProperty("end", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val end: Date,
    @JsonProperty("notes", required = true) override val notes: String,
) : TimeEntry {
    override val type: String = ACTIVITIES.SLEEP
    override val typeId: Int = ACTIVITIES.index(type)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TummyTimeEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("start", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val start: Date,
    @JsonProperty("end", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val end: Date,
    @JsonProperty("milestone", required = true) override val notes: String,
) : TimeEntry {
    override val type: String = ACTIVITIES.TUMMY_TIME
    override val typeId: Int = ACTIVITIES.index(type)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeedingEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("start", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val start: Date,
    @JsonProperty("end", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val end: Date,
    @JsonProperty("notes", required = true) override val notes: String,
    @JsonProperty("type", required = true) val feedingType: String,
    @JsonProperty("method", required = true) val feedingMethod: String,
    @JsonProperty("amount", required = true) val amount: Double,
) : TimeEntry {
    override val type: String = ACTIVITIES.FEEDING
    override val typeId: Int = ACTIVITIES.index(type)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PumpingEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("start", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val start: Date,
    @JsonProperty("end", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val end: Date,
    @JsonProperty("notes", required = true) override val notes: String,
    @JsonProperty("amount", required = true) val amount: Double,
) : TimeEntry {
    override val type: String = ACTIVITIES.PUMPING
    override val typeId: Int = ACTIVITIES.index(type)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChangeEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("time", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = true) override val notes: String,
    @JsonProperty("wet", required = true) val wet: Boolean,
    @JsonProperty("solid", required = true) val solid: Boolean,
    @JsonProperty("color", required = true) val color: String,
    @JsonProperty("amount", required = true) val amount: Double?,
) : TimeEntry {
    override val type: String = EVENTS.CHANGE
    override val typeId: Int = EVENTS.index(type)
    override val end: Date? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NoteEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("time", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val start: Date,
    @JsonProperty("note", required = true) override val notes: String,
) : TimeEntry {
    override val type: String = EVENTS.NOTE
    override val typeId: Int = EVENTS.index(type)
    override val end: Date? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BmiEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("date", required = true) @JsonDeserialize(using = DateOnlyDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = true) override val notes: String,
    @JsonProperty("bmi", required = true) val bmi: Double,
) : TimeEntry {
    override val type: String = EVENTS.BMI
    override val typeId: Int = EVENTS.index(type)
    override val end: Date? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TemperatureEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("time", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = true) override val notes: String,
    @JsonProperty("temperature", required = true) val temperature: Double,
) : TimeEntry {
    override val type: String = EVENTS.TEMPERATURE
    override val typeId: Int = EVENTS.index(type)
    override val end: Date? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeightEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("date", required = true) @JsonDeserialize(using = DateOnlyDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = true) override val notes: String,
    @JsonProperty("weight", required = true) val weight: Double,
) : TimeEntry {
    override val type: String = EVENTS.WEIGHT
    override val typeId: Int = EVENTS.index(type)
    override val end: Date? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HeightEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("date", required = true) @JsonDeserialize(using = DateOnlyDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = true) override val notes: String,
    @JsonProperty("height", required = true) val height: Double,
) : TimeEntry {
    override val type: String = EVENTS.HEIGHT
    override val typeId: Int = EVENTS.index(type)
    override val end: Date? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HeadCircumferenceEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("date", required = true) @JsonDeserialize(using = DateOnlyDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = true) override val notes: String,
    @JsonProperty("head_circumference", required = true) val head_circumference: Double,
) : TimeEntry {
    override val type: String = EVENTS.HEAD_CIRCUMFERENCE
    override val typeId: Int = EVENTS.index(type)
    override val end: Date? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaginatedEntries<T>(
    @JsonProperty("count", required = true)val count: Int,
    @JsonProperty("next", required = true) val nextUrl: String?,
    @JsonProperty("previous", required = true) val prevUrl: String?,
    @JsonProperty("results", required = true) val entries: List<T>,
)

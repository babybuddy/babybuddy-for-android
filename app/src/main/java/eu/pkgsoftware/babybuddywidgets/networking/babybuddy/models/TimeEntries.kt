package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.EVENTS
import eu.pkgsoftware.babybuddywidgets.DateTimeDeserializer
import eu.pkgsoftware.babybuddywidgets.DateOnlyDeserializer
import eu.pkgsoftware.babybuddywidgets.DateTimeSerializer
import java.util.Date
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

annotation class ActivityName(val name: String)
annotation class APIPath(val path: String)
annotation class UIPath(val path: String)

interface TimeEntry {
    val appType: String
    val appTypeId: Int
    val id: Int
    val childId: Int
    val start: Date
    val end: Date
    val notes: String
}


@UIPath("sleep")
@APIPath("sleep")
@ActivityName(ACTIVITIES.SLEEP)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SleepEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonDeserialize(using = DateTimeDeserializer::class)
    @JsonSerialize(using = DateTimeSerializer::class)
    @JsonProperty("start", required = true)
    override val start: Date,
    @JsonDeserialize(using = DateTimeDeserializer::class)
    @JsonSerialize(using = DateTimeSerializer::class)
    @JsonProperty("end", required = true)
    override val end: Date,
    @JsonSetter("notes") val _notes: String?,
) : TimeEntry {
    override @JsonIgnore val appType: String = ACTIVITIES.SLEEP
    override @JsonIgnore val appTypeId: Int = ACTIVITIES.index(appType)
    override @get:JsonGetter("notes") val notes: String = _notes ?: ""
}

@UIPath("tummy-time")
@APIPath("tummy-times")
@ActivityName(ACTIVITIES.TUMMY_TIME)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TummyTimeEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,

    @JsonDeserialize(using = DateTimeDeserializer::class)
    @JsonSerialize(using = DateTimeSerializer::class)
    @JsonProperty("start", required = true)
    override val start: Date,

    @JsonDeserialize(using = DateTimeDeserializer::class)
    @JsonSerialize(using = DateTimeSerializer::class)
    @JsonProperty("end", required = true)
    override val end: Date,

    @JsonSetter("milestone") val _notes: String?,
) : TimeEntry {
    override @JsonIgnore val appType: String = ACTIVITIES.TUMMY_TIME
    override @JsonIgnore val appTypeId: Int = ACTIVITIES.index(appType)
    override @get:JsonGetter("milestone") val notes: String = _notes ?: ""
}

@UIPath("feedings")
@APIPath("feedings")
@ActivityName(ACTIVITIES.FEEDING)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FeedingEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,

    @JsonDeserialize(using = DateTimeDeserializer::class)
    @JsonSerialize(using = DateTimeSerializer::class)
    @JsonProperty("start", required = true)
    override val start: Date,

    @JsonDeserialize(using = DateTimeDeserializer::class)
    @JsonSerialize(using = DateTimeSerializer::class)
    @JsonProperty("end", required = true)
    override val end: Date,

    @JsonSetter("notes") val _notes: String?,
    @JsonProperty("type", required = true) val feedingType: String,
    @JsonProperty("method", required = true) val feedingMethod: String,
    @JsonProperty("amount", required = true) val amount: Double?,
) : TimeEntry {
    override @JsonIgnore val appType: String = ACTIVITIES.FEEDING
    override @JsonIgnore val appTypeId: Int = ACTIVITIES.index(appType)
    override @get:JsonGetter("notes") val notes: String = _notes ?: ""
}

@UIPath("pumping")
@APIPath("pumping")
@ActivityName(ACTIVITIES.PUMPING)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PumpingEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("start", required = false) @JsonDeserialize(using = DateTimeDeserializer::class) val _start: Date?,
    @JsonProperty("end", required = false) @JsonDeserialize(using = DateTimeDeserializer::class) val _end: Date?,
    @JsonProperty("notes", required = false) val _notes: String?,
    @JsonProperty("amount", required = true) val amount: Double,
    @JsonProperty("time", required = false) @JsonDeserialize(using = DateTimeDeserializer::class) private val _legacyTime: Date?
) : TimeEntry {
    override @JsonIgnore val appType: String = ACTIVITIES.PUMPING
    override @JsonIgnore val appTypeId: Int = ACTIVITIES.index(appType)
    override val start: Date = _start ?: _legacyTime!!
    override val end: Date = _end ?: _legacyTime!!
    override val notes: String = _notes ?: ""
}

@UIPath("changes")
@APIPath("changes")
@ActivityName(EVENTS.CHANGE)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ChangeEntry(
    @JsonProperty("id", required = true)
    override val id: Int,

    @JsonProperty("child", required = true)
    override val childId: Int,

    @JsonProperty("time", required = true)
    @JsonDeserialize(using = DateTimeDeserializer::class)
    @JsonSerialize(using = DateTimeSerializer::class)
    override val start: Date,

    @JsonSetter("notes") val _notes: String?,
    @JsonProperty("wet", required = true) val wet: Boolean,
    @JsonProperty("solid", required = true) val solid: Boolean,
    @JsonProperty("color", required = true) val color: String,
    @JsonProperty("amount", required = true) val amount: Double?,
) : TimeEntry {
    override @JsonIgnore val appType: String = EVENTS.CHANGE
    override @JsonIgnore val appTypeId: Int = EVENTS.index(appType)
    override val end: Date = start
    override @get:JsonGetter("notes") val notes: String = _notes ?: ""
}

@UIPath("notes")
@APIPath("notes")
@ActivityName(EVENTS.NOTE)
@JsonIgnoreProperties(ignoreUnknown = true)
data class NoteEntry(
    @JsonProperty("id", required = true)
    override val id: Int,

    @JsonProperty("child", required = true)
    override val childId: Int,

    @JsonProperty("time", required = true)
    @JsonDeserialize(using = DateTimeDeserializer::class)
    @JsonSerialize(using = DateTimeSerializer::class)
    override val start: Date,

    @JsonProperty("note", required = false) val _notes: String?,
) : TimeEntry {
    override @JsonIgnore val appType: String = EVENTS.NOTE
    override @JsonIgnore val appTypeId: Int = EVENTS.index(appType)
    override val end: Date = start
    override val notes: String = _notes ?: ""
}

@UIPath("bmi")
@APIPath("bmi")
@ActivityName(EVENTS.BMI)
@JsonIgnoreProperties(ignoreUnknown = true)
data class BmiEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("date", required = true) @JsonDeserialize(using = DateOnlyDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = false) val _notes: String?,
    @JsonProperty("bmi", required = true) val bmi: Double,
) : TimeEntry {
    override @JsonIgnore val appType: String = EVENTS.BMI
    override @JsonIgnore val appTypeId: Int = EVENTS.index(appType)
    override val end: Date = start
    override val notes: String = _notes ?: ""
}

@UIPath("temperature")
@APIPath("temperature")
@ActivityName(EVENTS.TEMPERATURE)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TemperatureEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("time", required = true) @JsonDeserialize(using = DateTimeDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = false) val _notes: String?,
    @JsonProperty("temperature", required = true) val temperature: Double,
) : TimeEntry {
    override @JsonIgnore val appType: String = EVENTS.TEMPERATURE
    override @JsonIgnore val appTypeId: Int = EVENTS.index(appType)
    override val end: Date = start
    override val notes: String = _notes ?: ""
}

@UIPath("weight")
@APIPath("weight")
@ActivityName(EVENTS.WEIGHT)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WeightEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("date", required = true) @JsonDeserialize(using = DateOnlyDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = false) val _notes: String?,
    @JsonProperty("weight", required = true) val weight: Double,
) : TimeEntry {
    override @JsonIgnore val appType: String = EVENTS.WEIGHT
    override @JsonIgnore val appTypeId: Int = EVENTS.index(appType)
    override val end: Date = start
    override val notes: String = _notes ?: ""
}

@UIPath("height")
@APIPath("height")
@ActivityName(EVENTS.HEIGHT)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HeightEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("date", required = true) @JsonDeserialize(using = DateOnlyDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = false) val _notes: String?,
    @JsonProperty("height", required = true) val height: Double,
) : TimeEntry {
    override @JsonIgnore val appType: String = EVENTS.HEIGHT
    override @JsonIgnore val appTypeId: Int = EVENTS.index(appType)
    override val end: Date = start
    override val notes: String = _notes ?: ""
}

@UIPath("head-circumference")
@APIPath("head-circumference")
@ActivityName(EVENTS.HEAD_CIRCUMFERENCE)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HeadCircumferenceEntry(
    @JsonProperty("id", required = true) override val id: Int,
    @JsonProperty("child", required = true) override val childId: Int,
    @JsonProperty("date", required = true) @JsonDeserialize(using = DateOnlyDeserializer::class) override val start: Date,
    @JsonProperty("notes", required = false) val _notes: String?,
    @JsonProperty("head_circumference", required = true) val head_circumference: Double,
) : TimeEntry {
    override @JsonIgnore val appType: String = EVENTS.HEAD_CIRCUMFERENCE
    override @JsonIgnore val appTypeId: Int = EVENTS.index(appType)
    override val end: Date = start
    override val notes: String = _notes ?: ""
}

// Generic wrappers

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaginatedEntries<T>(
    @JsonProperty("count", required = true)val count: Int,
    @JsonProperty("next", required = true) val nextUrl: String?,
    @JsonProperty("previous", required = true) val prevUrl: String?,
    @JsonProperty("results", required = true) val entries: List<T>,
)

fun classActivityName(cls: KClass<*>): String {
    val a = cls.findAnnotations(ActivityName::class)
    if (a.isEmpty()) {
        throw IncorrectApiConfiguration("@ActivityName missing for ${cls.qualifiedName}")
    }
    return a[0].name;
}

fun classUIPath(cls: KClass<*>): String {
    val a = cls.findAnnotations(UIPath::class)
    if (a.isEmpty()) {
        throw IncorrectApiConfiguration("@UIPath missing for ${cls.qualifiedName}")
    }
    return a[0].path;
}


fun classAPIPath(cls: KClass<*>): String {
    val a = cls.findAnnotations(APIPath::class)
    if (a.isEmpty()) {
        throw IncorrectApiConfiguration("@APIPath missing for ${cls.qualifiedName}")
    }
    return a[0].path;
}

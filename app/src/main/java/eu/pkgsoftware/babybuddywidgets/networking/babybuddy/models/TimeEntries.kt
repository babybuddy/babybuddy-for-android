package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.DateDeserializer
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.OptDateDeserializer
import java.util.Date

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
data class SleepEntry(
    @JsonProperty("id") override val id: Int,
    @JsonProperty("child") override val childId: Int,
    @JsonProperty("start") @JsonDeserialize(using = DateDeserializer::class) override val start: Date,
    @JsonProperty("end") @JsonDeserialize(using = DateDeserializer::class) override val end: Date,
    @JsonProperty("notes") override val notes: String,
) : TimeEntry {
    override val type: String = ACTIVITIES.SLEEP
    override val typeId: Int = ACTIVITIES.index(type)
}
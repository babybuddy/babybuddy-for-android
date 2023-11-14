package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.AnyDateTimeDeserializer
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class Child(
    @JsonProperty("id", required = true) val id: Int,
    @JsonProperty("first_name", required = true) val firstName: String,
    @JsonProperty("last_name", required = true) val lastName: String,
    @JsonProperty("birth_date", required = true) @JsonDeserialize(using = AnyDateTimeDeserializer::class) val birthData: Date,
    @JsonProperty("slug", required = true) val slug: String,
    @JsonProperty("picture", required = true) val picture: String?,
)

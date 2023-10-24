package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import com.fasterxml.jackson.annotation.JsonProperty

data class UserSubstruct(
    @JsonProperty("id") val id: Int,
    @JsonProperty("username") val username: String,
    @JsonProperty("first_name") val firstName: String,
    @JsonProperty("last_name") val lastName: String,
    @JsonProperty("email") val email: String,
    @JsonProperty("is_staff") val isStaff: Boolean
)

data class Profile(
    @JsonProperty("user") val user: UserSubstruct,
    @JsonProperty("language") val language: String,
    @JsonProperty("timezone") val timezone: String,
    @JsonProperty("api_key") val apiKey: String
)

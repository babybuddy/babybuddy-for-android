package eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import eu.pkgsoftware.babybuddywidgets.AnyDateTimeDeserializer
import java.util.Date

fun childIndexBySlug(children: Array<Child>, slug: String): Int {
    var i = 0
    for (c in children) {
        if (c.slug == slug) {
            return i
        }
        i++
    }
    return -1
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Child(
    @JsonProperty("id", required = true) val id: Int,
    @JsonProperty("first_name", required = true) val firstName: String,
    @JsonProperty("last_name", required = true) val lastName: String,
    @JsonProperty("birth_date", required = true) @JsonDeserialize(using = AnyDateTimeDeserializer::class) val birthData: Date,
    @JsonProperty("slug", required = true) val slug: String,
    @JsonProperty("picture", required = true) val picture: String?,
) {
    companion object {
        fun childIndexBySlug(children: Array<Child>, slug: String): Int {
            return eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.childIndexBySlug(children, slug)
        }
    }
}

data class ChildrenList (
    @JsonProperty("children", required = true) val children: Array<Child>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChildrenList
        return children.contentEquals(other.children)
    }

    override fun hashCode(): Int {
        return children.contentHashCode()
    }
}

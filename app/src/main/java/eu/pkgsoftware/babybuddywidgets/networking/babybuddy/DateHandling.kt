package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

var SystemServerTimeOffset = -1000L

val DATE_TIME_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssX"
val DATE_ONLY_FORMAT_STRING = "yyyy-MM-dd"

fun parseNullOrDate(s: String, format: String): Date? {
    val sdf = SimpleDateFormat(format)

    try {
        var strDate = s
        // Remove milliseconds
        strDate = strDate.replace("\\.[0-9]+([+-Z])".toRegex(), "$1")
        // Normalize UTC
        strDate = strDate.replace("Z$".toRegex(), "+00:00")
        return sdf.parse(strDate)
    }
    catch (e: ParseException) {
        return null
    }
}

fun clientToServerTime(d: Date): Date {
    return Date(d.time + SystemServerTimeOffset)
}

fun serverTimeToClientTime(d: Date): Date {
    return Date(d.time - SystemServerTimeOffset)
}


class DateTimeDeserializer : StdDeserializer<Date>(Date::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Date {
        p.text?.let {
            parseNullOrDate(it, DATE_TIME_FORMAT_STRING)?.let {
                return serverTimeToClientTime(it)
            }
        }
        throw IOException("Invalid date string ${p.text}")
    }
}

class DateOnlyDeserializer : StdDeserializer<Date>(Date::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Date {
        p.text?.let {
            parseNullOrDate(it, DATE_ONLY_FORMAT_STRING)?.let {
                return it
            }
        }
        throw IOException("Invalid date string ${p.text}")
    }
}

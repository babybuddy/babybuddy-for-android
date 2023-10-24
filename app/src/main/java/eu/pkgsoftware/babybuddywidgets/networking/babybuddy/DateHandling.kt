package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

var SystemServerTimeOffset = -1000L

val DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssX"

fun parseNullOrDate(s: String): Date? {
    val sdf = SimpleDateFormat(BabyBuddyClient.DATE_FORMAT_STRING)

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


class DateDeserializer : StdDeserializer<Date>(Date::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Date {
        p.text?.let {
            parseNullOrDate(it)?.let {
                return serverTimeToClientTime(it)
            }
        }
        throw IOException("Invalid date string ${p.text}")
    }
}

class OptDateDeserializer : StdDeserializer<Date?>(Date::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Date? {
        if (p.currentToken == JsonToken.VALUE_NULL) {
            return null
        }
        p.text?.let {
            parseNullOrDate(it)?.let {
                return serverTimeToClientTime(it)
            }
        }
        throw IOException("Invalid date string ${p.text}")
    }
}


package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

var SystemServerTimeOffset = -1000L

val DATE_TIME_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssX"
val DATE_ONLY_FORMAT_STRING = "yyyy-MM-dd"

fun parseNullOrDate(s: String, format: String): Date? {
    val sdf = SimpleDateFormat(format, Locale.ENGLISH)

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

fun formatDate(d: Date, format: String): String {
    val sdf = SimpleDateFormat(format, Locale.ENGLISH)
    return sdf.format(d)
}

fun clientToServerTime(d: Date): Date {
    return Date(d.time + SystemServerTimeOffset)
}

fun serverTimeToClientTime(d: Date): Date {
    return Date(d.time - SystemServerTimeOffset)
}

fun nowServer(): Date {
    return clientToServerTime(Date())
}

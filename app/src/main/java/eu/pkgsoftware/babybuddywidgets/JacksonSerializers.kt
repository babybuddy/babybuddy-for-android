package eu.pkgsoftware.babybuddywidgets

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.DATE_ONLY_FORMAT_STRING
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.DATE_TIME_FORMAT_STRING
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.parseNullOrDate
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.serverTimeToClientTime
import java.io.IOException
import java.util.Date

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

class AnyDateTimeDeserializer : StdDeserializer<Date>(Date::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Date {
        p.text?.let {
            parseNullOrDate(it, DATE_TIME_FORMAT_STRING)?.let {
                return serverTimeToClientTime(it)
            }
            parseNullOrDate(it, DATE_ONLY_FORMAT_STRING)?.let {
                return it
            }
        }
        throw IOException("Invalid date string ${p.text}")
    }
}

class StringArrayDeserializer : StdDeserializer<Array<String>>(Array<String>::class.java) {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Array<String> {
        val list = mutableListOf<String>()
        p?.readValuesAs(String::class.java)?.forEach {
            list.add(it)
        }
        return list.toTypedArray()
    }

}

class StringArraySerializer : StdSerializer<Array<String>>(Array<String>::class.java) {
    override fun serialize(
        value: Array<String>?,
        gen: JsonGenerator?,
        provider: SerializerProvider?
    ) {
        gen?.writeStartArray()
        value?.forEach {
            gen?.writeString(it)
        }
        gen?.writeEndArray()
    }
}

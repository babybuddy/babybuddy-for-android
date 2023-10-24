package eu.pkgsoftware.babybuddywidgets

import com.fasterxml.jackson.databind.ObjectMapper
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.SleepEntry
import org.junit.Test

class ClientV2Test {
    @Test
    fun sleepObjectSerialization() {
        val s: String = """{
            "id": 269,
            "child": 1,
            "start": "2023-10-13T00:07:08.351000+02:00",
            "end": "2023-10-13T00:07:12.107094+02:00",
            "duration": "00:00:03.756094",
            "nap": false,
            "notes": "",
            "tags": []
        }"""

        val mapper = ObjectMapper()
        val sleepEntry: SleepEntry = mapper.readValue(s, SleepEntry::class.java)
    }
}
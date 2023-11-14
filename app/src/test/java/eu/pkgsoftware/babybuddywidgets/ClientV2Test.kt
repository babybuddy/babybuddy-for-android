package eu.pkgsoftware.babybuddywidgets

import com.fasterxml.jackson.databind.ObjectMapper
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.BmiEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.ChangeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.FeedingEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.HeadCircumferenceEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.HeightEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.NoteEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.PumpingEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.SleepEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TemperatureEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.TummyTimeEntry
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.models.WeightEntry
import org.junit.Test

class ClientV2Test {
    @Test
    fun sleepObjectSerialization() {
        val s: String = """
        {
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

    @Test
    fun tummyTimeObjectSerialization() {
        val s: String = """
        {
            "id": 230,
            "child": 1,
            "start": "2023-10-13T00:07:20.216000+02:00",
            "end": "2023-10-13T00:07:29.496101+02:00",
            "duration": "00:00:09.280101",
            "milestone": "Pierre is nice",
            "tags": []
        }"""

        val mapper = ObjectMapper()
        val tummyTime: TummyTimeEntry = mapper.readValue(s, TummyTimeEntry::class.java)
    }

    @Test
    fun feedingObjectSerialization() {
        val s: String = """
        {
            "id": 223,
            "child": 1,
            "start": "2023-10-13T00:07:10.249000+02:00",
            "end": "2023-10-13T00:07:16.967648+02:00",
            "duration": "00:00:06.718648",
            "type": "breast milk",
            "method": "right breast",
            "amount": null,
            "notes": "",
            "tags": []
        }"""

        val mapper = ObjectMapper()
        val feeding: FeedingEntry = mapper.readValue(s, FeedingEntry::class.java)
    }

    @Test
    fun pumpingObjectSerialization() {
        val s: String = """
        {
            "id": 2,
            "child": 1,
            "amount": 0.0,
            "start": "2023-10-24T23:49:40+02:00",
            "end": "2023-10-24T23:55:40+02:00",
            "duration": "00:06:00",
            "notes": "",
            "tags": [
                "Listened to Music"
            ]
        }"""

        val mapper = ObjectMapper()
        val pumping: PumpingEntry = mapper.readValue(s, PumpingEntry::class.java)
    }

    @Test
    fun changeObjectSerialization() {
        val s: String = """
        {
            "id": 404,
            "child": 1,
            "time": "2023-08-31T23:51:27+02:00",
            "wet": true,
            "solid": false,
            "color": "",
            "amount": null,
            "notes": "",
            "tags": [
               "Happy"
            ]
        }"""

        val mapper = ObjectMapper()
        val change: ChangeEntry = mapper.readValue(s, ChangeEntry::class.java)
    }

    @Test
    fun noteObjectSerialization() {
        val s: String = """
        {
            "id": 3,
            "child": 1,
            "note": "Hello",
            "time": "2022-05-26T16:53:00+02:00",
            "tags": [
                "Holla"
            ]
        }"""

        val mapper = ObjectMapper()
        val note: NoteEntry = mapper.readValue(s, NoteEntry::class.java)
    }

    @Test
    fun bmiObjectSerialization() {
        val s: String = """
        {
            "id": 5,
            "child": 1,
            "bmi": 11.18,
            "date": "2022-03-05",
            "notes": "Number sit seven note.",
            "tags": []
        }"""

        val mapper = ObjectMapper()
        val bmi: BmiEntry = mapper.readValue(s, BmiEntry::class.java)
    }

    @Test
    fun temperatureObjectSerialization() {
        val s: String = """
        {
            "id": 78,
            "child": 1,
            "temperature": 101.96,
            "time": "2022-03-07T21:09:00+01:00",
            "notes": "Why special keep bag song nice lawyer strategy.",
            "tags": [
                "4re"
            ]
        }"""

        val mapper = ObjectMapper()
        val temperature: TemperatureEntry = mapper.readValue(s, TemperatureEntry::class.java)
    }

    @Test
    fun weightObjectSerialization() {
        val s: String = """
        {
            "id": 5,
            "child": 1,
            "weight": 12.9,
            "date": "2022-03-05",
            "notes": "",
            "tags": [
                "a tag"
            ]
        }"""

        val mapper = ObjectMapper()
        val weight: WeightEntry = mapper.readValue(s, WeightEntry::class.java)
    }

    @Test
    fun heightObjectSerialization() {
        val s: String = """
        {
            "id": 5,
            "child": 1,
            "height": 9.43,
            "date": "2022-03-05",
            "notes": "",
            "tags": []
        }"""

        val mapper = ObjectMapper()
        val height: HeightEntry = mapper.readValue(s, HeightEntry::class.java)
    }

    @Test
    fun headCircumferenceObjectSerialization() {
        val s: String = """
        {
            "id": 5,
            "child": 1,
            "head_circumference": 12.12,
            "date": "2022-03-05",
            "notes": "",
            "tags": [
                "Holla"
            ]
        }"""

        val mapper = ObjectMapper()
        val headcircumference: HeadCircumferenceEntry = mapper.readValue(s, HeadCircumferenceEntry::class.java)
    }
}

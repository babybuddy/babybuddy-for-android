package eu.pkgsoftware.babybuddywidgets

import eu.pkgsoftware.babybuddywidgets.login.GrabAppToken
import eu.pkgsoftware.babybuddywidgets.login.GrabAppToken.MissingPage
import eu.pkgsoftware.babybuddywidgets.networking.ServerAccessProviderInterface
import eu.pkgsoftware.babybuddywidgets.networking.babybuddy.Client
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.net.URL

data class ServerUrl(val version: String, val url: String)

class ClientV2IntegrationTest {
    var serverUrlArray = mutableListOf<ServerUrl>()

    init {
        val path = File("../tools/babybuddy-version-array/versions")
        var portIndex = 9000
        for (l in path.readLines()) {
            val line = l.trim()
            if (line.length == 0) continue
            if (line.startsWith("#")) continue
            serverUrlArray.add(
                ServerUrl(line, "http://localhost:$portIndex")
            )
            portIndex += 1
        }
    }

    var serverUrl = ""

    inner class AppInterface : ServerAccessProviderInterface {
        private val _serverUrl = this@ClientV2IntegrationTest.serverUrl
        private var _appToken = ""

        init {
            val gat = GrabAppToken(URL(serverUrl))
            gat.login("testuser", "testuser")
            try {
                _appToken = gat.fromProfilePage
            }
            catch (e: MissingPage) {
                _appToken = gat.parseFromSettingsPage()
            }
        }

        override fun getAppToken(): String {
            return _appToken
        }

        override fun getServerUrl(): String {
            return _serverUrl
        }
    }

    suspend fun runObtainListsTest() {
        val client = Client(AppInterface())

        println("Get profile")
        client.getProfile()

        println("Get tummy-times")
        val tummyTimes = client.getEntries(TummyTimeEntry::class, 0, 100)
        Assert.assertTrue(tummyTimes.totalCount > 0)
        Assert.assertTrue(tummyTimes.entries.size > 0)

        println("Get sleep entries")
        val sleepEntries = client.getEntries(SleepEntry::class, 0, 100)
        Assert.assertTrue(sleepEntries.totalCount > 0)
        Assert.assertTrue(sleepEntries.entries.size > 0)

        println("Get feeding entries")
        val feedingEntries = client.getEntries(FeedingEntry::class, 0, 100)
        Assert.assertTrue(feedingEntries.totalCount > 0)
        Assert.assertTrue(feedingEntries.entries.size > 0)

        println("Get pumping entries")
        val pumpingEntries = client.getEntries(PumpingEntry::class, 0, 100)
        Assert.assertTrue(pumpingEntries.totalCount > 0)
        Assert.assertTrue(pumpingEntries.entries.size > 0)

        println("Get change entries")
        val changeEntires = client.getEntries(ChangeEntry::class, 0, 100)
        Assert.assertTrue(changeEntires.totalCount > 0)
        Assert.assertTrue(changeEntires.entries.size > 0)

        println("Get notes")
        val notes = client.getEntries(NoteEntry::class, 0, 100)
        Assert.assertTrue(notes.totalCount > 0)
        Assert.assertTrue(notes.entries.size > 0)

        println("Get bmi entries")
        val bmiEntries = client.getEntries(BmiEntry::class, 0, 100)
        Assert.assertTrue(bmiEntries.totalCount > 0)
        Assert.assertTrue(bmiEntries.entries.size > 0)

        println("Get temperature entries")
        val tempEntries = client.getEntries(TemperatureEntry::class, 0, 100)
        Assert.assertTrue(tempEntries.totalCount > 0)
        Assert.assertTrue(tempEntries.entries.size > 0)

        println("Get weight entries")
        val weightEntries = client.getEntries(WeightEntry::class, 0, 100)
        Assert.assertTrue(weightEntries.totalCount > 0)
        Assert.assertTrue(weightEntries.entries.size > 0)

        println("Get height entries")
        val heightEntries = client.getEntries(HeightEntry::class, 0, 100)
        Assert.assertTrue(heightEntries.totalCount > 0)
        Assert.assertTrue(heightEntries.entries.size > 0)

        println("Get head circumference entries")
        val headCircEntires = client.getEntries(HeadCircumferenceEntry::class, 0, 100)
        Assert.assertTrue(headCircEntires.totalCount > 0)
        Assert.assertTrue(headCircEntires.entries.size > 0)
    }

    @Test
    fun testObtainLists() = runTest {
        for (server in serverUrlArray) {
            serverUrl = server.url
            println("TESTING BABYBUDDY VERSION: ${server.version}")
            runObtainListsTest()
        }
    }
}
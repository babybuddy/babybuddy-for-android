package eu.pkgsoftware.babybuddywidgets

import eu.pkgsoftware.babybuddywidgets.networking.ServerTimeOffsetTracker
import org.junit.Assert
import org.junit.Test
import java.util.Date

class TestableServerTimeOffsetTracker : ServerTimeOffsetTracker() {
    var testTime = 0L

    override fun currentTimeMillis(): Long {
        return testTime
    }
}

class ServerTimeOffsetCorrectionTest {
    fun headerFromMillis(now: Long): String {
        return Constants.SERVER_DATE_FORMAT.format(Date(now))
    }

    @Test
    fun serverLaggingBehindClientUniform() {
        val tracker = TestableServerTimeOffsetTracker()
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-1000))
        tracker.updateServerTime(headerFromMillis(-2000))

        Assert.assertEquals(-1000, tracker.measuredOffset)
        Assert.assertEquals(8000, tracker.localToServerTime(10000))
    }

    @Test
    fun serverAheadOfClientUniform() {
        val tracker = TestableServerTimeOffsetTracker()
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(1000))
        tracker.updateServerTime(headerFromMillis(2000))

        Assert.assertEquals(1000, tracker.measuredOffset)
        Assert.assertEquals(10000, tracker.localToServerTime(10000))
    }

    @Test
    fun serverLaggingBehindRamp() {
        val tracker = TestableServerTimeOffsetTracker()
        for (i in 0 .. 9) {
            tracker.updateServerTime(headerFromMillis(-5000L + i * 1000))
        }

        Assert.assertEquals(-5000, tracker.measuredOffset)
        Assert.assertEquals(4000, tracker.localToServerTime(10000))
    }
}
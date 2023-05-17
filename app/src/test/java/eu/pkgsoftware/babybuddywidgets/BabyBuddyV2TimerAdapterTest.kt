package eu.pkgsoftware.babybuddywidgets

import eu.pkgsoftware.babybuddywidgets.compat.BabyBuddyV2TimerAdapter
import eu.pkgsoftware.babybuddywidgets.networking.BabyBuddyClient.ACTIVITIES
import org.junit.Assert
import org.junit.Test

class BabyBuddyV2TimerAdapterTest {
    @Test
    fun mapNames() {
        Assert.assertEquals(
            null,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity("XYXnonsense")
        )

        Assert.assertEquals(
            ACTIVITIES.TUMMY_TIME,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity(ACTIVITIES.TUMMY_TIME)
        )

        Assert.assertEquals(
            ACTIVITIES.SLEEP,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity("sleep")
        )

        Assert.assertEquals(
            ACTIVITIES.SLEEP,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity("   Sleep\n\t")
        )

        Assert.assertEquals(
            ACTIVITIES.FEEDING,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity("XYXnonsense-BBapp:1")
        )

        Assert.assertEquals(
            ACTIVITIES.FEEDING,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity("sleep-BBapp:1")
        )

        Assert.assertEquals(
            ACTIVITIES.SLEEP,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity("XYXnonsense-BBapp:2")
        )

        Assert.assertEquals(
            ACTIVITIES.TUMMY_TIME,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity("XYXnonsense-BBapp:3")
        )

        Assert.assertEquals(
            ACTIVITIES.TUMMY_TIME,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity("  XYXnonsense-bbapp:3 ")
        )

        Assert.assertEquals(
            null,
            BabyBuddyV2TimerAdapter.mapBabyBuddyNameToActivity(
                "XYXnonsense-BBapp:${ACTIVITIES.ALL.size + 1}"
            )
        )
    }
}
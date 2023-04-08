package eu.pkgsoftware.babybuddywidgets

import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListIntegrator
import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListItem
import eu.pkgsoftware.babybuddywidgets.logic.EndAwareContinuousListIntegrator
import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class EndAwareListIntegratorTest {
    fun createItemList(
        className: String, count: Int, start: Long, increment: Long, idStart: Int
    ): Array<ContinuousListItem> {
        val result = mutableListOf<ContinuousListItem>()
        for (i in 0..count - 1) {
            result.add(ContinuousListItem(start + i * increment, className, "id-${idStart + i}"))
        }
        return result.toTypedArray()
    }

    fun assertArrayEqualsWithDirty(a1: Array<ContinuousListItem>, a2: Array<ContinuousListItem>) {
        Assert.assertArrayEquals(a1, a2)
        a1.zip(a2).forEach {
            Assert.assertEquals(it.first.dirty, it.second.dirty)
        }
    }

    @Test
    fun emptyItemsTest() {
        val tested = EndAwareContinuousListIntegrator()

        Assert.assertEquals(0, tested.computeValidCount())
        tested.updateItemsWithCount(100, 0, "A", arrayOf())
        Assert.assertEquals(0, tested.computeValidCount())
    }

    @Test
    fun emptyClassDoesNotInfluenceValidOffset() {
        val tested = EndAwareContinuousListIntegrator()

        val aItems = createItemList("A", 5, 0, 1000, 0)
        tested.updateItemsWithCount(10, 0, "A", aItems)
        tested.updateItemsWithCount(0, 0, "B", arrayOf())

        Assert.assertEquals(5, tested.computeValidCount())
   }

    @Test
    fun updateValidCountSingleClass() {
        val tested = EndAwareContinuousListIntegrator()

        val aItems = createItemList("A", 10, 0, 1000, 0)
        tested.updateItemsWithCount(20, 0, "A", aItems)

        Assert.assertEquals(10, tested.computeValidCount())

        tested.updateItemsWithCount(20, 5, "A", aItems)
        Assert.assertEquals(15, tested.computeValidCount())

        val aItems2 = createItemList("A", 1, 0, 1000, 0)
        tested.updateItemsWithCount(20, 0, "A", aItems2)
        Assert.assertEquals(1, tested.computeValidCount())
    }

    @Test
    fun updateComplexMulticlass() {
        val tested = EndAwareContinuousListIntegrator()

        val aItems = createItemList("A", 5, 0, 1000, 0)
        val bItems = createItemList("B", 2, 1100, 1000, 0)
        // Order: A A B A B A A
        //                 ^ Starting here, list should be counted as "invalid" if b is "longer"

        tested.updateItemsWithCount(5, 0, "A", aItems)
        tested.updateItemsWithCount(10, 0, "B", bItems)

        Assert.assertEquals(5, tested.computeValidCount())

        // Order: A A B A B A A
        //                     ^ Full list should be valid if "totalCount" of B is 2
        tested.updateItemsWithCount(2, 0, "B", bItems)

        Assert.assertEquals(7, tested.computeValidCount())
    }
}
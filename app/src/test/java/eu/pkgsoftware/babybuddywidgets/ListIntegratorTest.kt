package eu.pkgsoftware.babybuddywidgets

import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListIntegrator
import eu.pkgsoftware.babybuddywidgets.logic.ContinuousListItem
import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ListIntegratorTest {
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
    fun obtainAndUpdateListWorks() {
        val tested = ContinuousListIntegrator()
        Assert.assertArrayEquals(tested.items, arrayOf<ContinuousListItem>())

        val classAItems1 = createItemList(
            "classA", 10, 0, 1000, 1
        )
        val classAItems2 = createItemList(
            "classA", 10, 5000, 1000, 6
        )
        val combined = classAItems1.toMutableList()
        combined.removeIf {
            it.orderNumber >= 5000
        }
        combined.addAll(classAItems2)

        tested.updateItems(0, classAItems1)
        assertArrayEqualsWithDirty(classAItems1, tested.items)
        tested.updateItems(5, classAItems2)
        assertArrayEqualsWithDirty(combined.toTypedArray(), tested.items)

        // Reapplying an already existing and equal range will do nothing
        tested.updateItems(5, classAItems2)
        assertArrayEqualsWithDirty(combined.toTypedArray(), tested.items)

        // Applying a list with fewer items than the total list will truncate the list
        tested.updateItems(0, classAItems1)
        assertArrayEqualsWithDirty(classAItems1, tested.items)

        // Shifting the list up even by a single item will make the upper list "dirty"
        // and truncate the list below
        tested.updateItems(3, arrayOf(classAItems1[4]))
        Assert.assertArrayEquals(
            arrayOf(true, true, true, false),
            tested.items.map { it.dirty }.toTypedArray()
        )
    }
}
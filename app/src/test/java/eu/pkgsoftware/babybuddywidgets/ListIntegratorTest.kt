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

        // Adding a completely new item at an index overwrites that index
        val classANewItems = createItemList(
            "classA", 3, 0, 1000, 10
        )
        tested.updateItems(2, classANewItems)
        Assert.assertArrayEquals(
            arrayOf(true, true, false, false, false),
            tested.items.map { it.dirty }.toTypedArray()
        )
        assertArrayEqualsWithDirty(classANewItems, tested.items.sliceArray(2..4))

        // Clear the table - removes everything
        tested.clear()
        Assert.assertArrayEquals(arrayOf<ContinuousListItem>(), tested.items)
    }

    @Test
    fun modifyingListItemsUpdatesItems() {
        val testItems = createItemList("test", 10, 0, 1000, 1)
        val tested = ContinuousListIntegrator()

        tested.updateItems(0, testItems)
        Assert.assertArrayEquals(testItems, tested.items)

        val modTested = testItems.toMutableList()
        val replaced = modTested.removeAt(3)
        modTested.add(
            2, ContinuousListItem(
                modTested[2].orderNumber - 100,
                replaced.className,
                replaced.id
            )
        )
        val shouldBe = modTested.toTypedArray()
        modTested.removeAt(0)

        tested.updateItems(1, modTested.toTypedArray())
        Assert.assertArrayEquals(shouldBe, tested.items)
    }

    @Test
    fun mixingItems() {
        val aItems = createItemList("A", 10, 0, 1000, 1)
        val bItems = createItemList("B", 10, 500, 1000, 1)
        val tested = ContinuousListIntegrator()

        tested.updateItems(0, aItems)
        tested.updateItems(0, bItems)

        Assert.assertEquals(20, tested.items.size)
        Assert.assertArrayEquals(
            arrayOf("A", "B", "A", "B", "A", "B"),
            tested.items.sliceArray(0..5).map { it.className }.toTypedArray()
        )

        // Test padding
        val cItems = createItemList("C", 10, 250, 1000, 1)
        tested.updateItems(3, cItems)

        Assert.assertArrayEquals(
            arrayOf("A", "C", "C", "C", "C", "B", "A", "C", "B", "A", "C", "B"),
            tested.items.sliceArray(0..11).map { it.className }.toTypedArray()
        )
        Assert.assertArrayEquals(
            arrayOf(false, true, true, true, false, false, false, false),
            tested.items.sliceArray(0..7).map { it.dirty }.toTypedArray()
        )
    }

    @Test
    fun selection() {
        val aItems = createItemList("A", 10, 0, 1000, 1)
        val bItems = createItemList("B", 10, 500, 1000, 1)
        val tested = ContinuousListIntegrator()
        tested.updateItems(0, aItems)
        tested.updateItems(0, bItems)

        Assert.assertEquals(tested.top, aItems[0])
        Assert.assertArrayEquals(
            arrayOf(aItems[0], bItems[0], aItems[1]),
            tested.nElementsFromTop(0, 3)
        )
        Assert.assertArrayEquals(
            arrayOf(aItems[0], bItems[0], aItems[1]),
            tested.nElementsFromTop(-1, 3)
        )

        tested.selectTop(1500)
        Assert.assertEquals(tested.top, bItems[1])
        Assert.assertArrayEquals(
            arrayOf(bItems[1], aItems[2], bItems[2]),
            tested.nElementsFromTop(0, 3)
        )
        Assert.assertArrayEquals(
            arrayOf(aItems[0], bItems[0], aItems[1]),
            tested.nElementsFromTop(-3, 3)
        )

        tested.top = bItems[6]
        Assert.assertEquals(tested.top, bItems[6])
        Assert.assertArrayEquals(
            arrayOf(bItems[6], aItems[7], bItems[7]),
            tested.nElementsFromTop(0, 3)
        )

        tested.top = aItems[9]
        Assert.assertArrayEquals(
            arrayOf(aItems[9], bItems[9]),
            tested.nElementsFromTop(0, 3)
        )
    }

    @Test
    fun classCounts() {
        val aItems = createItemList("A", 5, 0, 1000, 1)
        val bItems = createItemList("B", 10, 500, 1000, 1)

        val tested = ContinuousListIntegrator()
        tested.updateItems(5, aItems)
        tested.updateItems(5, bItems)

        Assert.assertEquals(10, tested.classElementCount("A"))
        Assert.assertEquals(15, tested.classElementCount("B"))
    }

    @Test
    fun suggestedClassQueries() {
        val tested = ContinuousListIntegrator()
        Assert.assertEquals(0, tested.suggestClassQueryOffset("A"))

        val aItems = createItemList("A", 5, 0, 1000, 1)
        val bItems = createItemList("B", 10, 200, 1000, 1)
        tested.updateItems(10, aItems)
        tested.updateItems(10, bItems)

        tested.top = aItems[3]
        Assert.assertEquals(13, tested.suggestClassQueryOffset("A"))
        Assert.assertEquals(13, tested.suggestClassQueryOffset("B"))
        Assert.assertEquals(0, tested.suggestClassQueryOffset("C"))
    }
}
package eu.pkgsoftware.babybuddywidgets.logic

class ContinuousListItem(val orderNumber: Long, val className: String, val id: String?) {
    var dirty = false
    var userdata: Any? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is ContinuousListItem) {
            return false;
        }

        if ((other.id == null) || (id == null)) {
            return false
        }

        return orderNumber == other.orderNumber && className == other.className && id == other.id
    }

    override fun hashCode(): Int {
        return orderNumber.hashCode() * 3 + className.hashCode() * 5 + id.hashCode() * 7
    }

    override fun toString(): String {
        return "<ContinuousListItem on=${orderNumber} class=${className} id=${id}>"
    }
}

open class ContinuousListIntegrator {
    private var topOffset = Long.MIN_VALUE
    private val listItems = mutableListOf<ContinuousListItem>()

    val items get() = listItems.toTypedArray()
    val nonDirtyItems get() = listItems.filter { !it.dirty }.toTypedArray()
    var top: ContinuousListItem?
        get() = nonDirtyItems.minByOrNull { Math.abs(it.orderNumber - topOffset) }
        set(v) {
            if (v == null) {
                selectTop(Long.MIN_VALUE)
            } else {
                selectTop(v.orderNumber)
            }
        }

    fun selectTop(orderNumber: Long) {
        topOffset = orderNumber
    }

    private fun newDummy(order: Long, className: String): ContinuousListItem {
        val dummy = ContinuousListItem(order, className, null)
        dummy.dirty = true
        return dummy
    }

    open fun updateItems(listOffset: Int, className: String, items: Array<ContinuousListItem>) {
        val currentItems = listItems.filter { it.className == className }
        if (items.size > 0) {
            integrateListWithItems(currentItems, items, listOffset, className)
        } else {
            integrateEmptyList(currentItems, listOffset, className)
        }
        listItems.sortBy { it.orderNumber }
    }

    private fun integrateEmptyList(
        currentItems: List<ContinuousListItem>,
        listOffset: Int,
        className: String
    ) {
        val lastOrderNumbers =
            if (currentItems.isNotEmpty()) {
                currentItems.last().orderNumber
            } else if (listItems.isNotEmpty()) {
                listItems.last().orderNumber
            } else {
                Long.MIN_VALUE
            };
        if (listOffset > currentItems.size) {
            listItems.addAll((0 until listOffset - currentItems.size).map {
                newDummy(lastOrderNumbers, className)
            })
        } else if (listOffset < currentItems.size) {
            listItems.removeAll(currentItems.slice(listOffset until currentItems.size))
        }
    }

    private fun integrateListWithItems(
        currentItems: List<ContinuousListItem>,
        items: Array<ContinuousListItem>,
        listOffset: Int,
        className: String
    ) {
        val foundOffset =
            if (currentItems.isNotEmpty()) {
                currentItems.indexOf(items[0])
            } else {
                -1
            };
        val initialOrderNumber =
            if (items.isNotEmpty()) {
                items[0].orderNumber
            } else if (listItems.size > 0) {
                listItems[0].orderNumber
            } else {
                Long.MIN_VALUE
            };
        if (foundOffset < 0) {
            // We have nothing to go off, we need to trust the listOffset itself and pad everything with dummy values
            listItems.removeAll(currentItems)
            listItems.addAll((0 until listOffset).map {
                newDummy(initialOrderNumber, className)
            })
            listItems.addAll(items)
        } else if (foundOffset == listOffset) {
            // All good, we can go and combine things
            var equalLen = 0
            val foundOffsetRemainder = currentItems.size - foundOffset
            for (i in 0 until Math.min(foundOffsetRemainder, items.size)) {
                if (currentItems[foundOffset + i] != items[i]) {
                    break
                }
                equalLen = i + 1
            }

            listItems.removeAll(
                currentItems.slice(foundOffset + equalLen until currentItems.size)
            )
            listItems.addAll(items.slice(equalLen until items.size))
        } else {
            listItems.removeAll(currentItems)
            listItems.addAll((0 until listOffset).map {
                newDummy(initialOrderNumber, className)
            })
            listItems.addAll(items)
        }
    }

    open fun clear() {
        top = null
        listItems.clear()
    }

    fun classElementCount(className: String): Int {
        return listItems.count { it.className == className }
    }

    fun nElementsFromTop(startOffset: Int, n: Int): Array<ContinuousListItem> {
        val topOffset = listItems.indexOf(this.top)
        if (topOffset == -1) {
            return arrayOf()
        }

        val start = Math.min(listItems.size - 1, Math.max(0, topOffset + startOffset))
        val count = Math.min(n, Math.max(0, listItems.size - (topOffset + startOffset)))
        return listItems.slice(start until start + count).toTypedArray()
    }

    fun suggestClassQueryOffset(className: String): Int {
        val currentItems = listItems.filter { it.className == className }

        val selected = currentItems.minByOrNull { Math.abs(it.orderNumber - topOffset) }
        if (selected == null) {
            return 0
        }

        return currentItems.indexOf(selected)
    }
}
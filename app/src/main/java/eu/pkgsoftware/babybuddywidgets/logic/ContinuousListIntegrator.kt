package eu.pkgsoftware.babybuddywidgets.logic

class ContinuousListItem(val orderNumber: Long, val className: String, val id: String) {
    var dirty = false

    override fun equals(other: Any?): Boolean {
        if (other !is ContinuousListItem) {
            return false;
        }

        return orderNumber == other.orderNumber && className == other.className && id == other.id
    }

    override fun hashCode(): Int {
        return orderNumber.hashCode() * 3 + className.hashCode() * 5 + id.hashCode() * 7
    }
}

class ContinuousListIntegrator {
    private var top: ContinuousListItem? = null
    private val listItems = mutableListOf<ContinuousListItem>()
    private val anonymousByClass = mutableMapOf<String, Int>()

    val items get() = listItems.toTypedArray()
    val nonDirtyItems get() = listItems.filter { !it.dirty }.toTypedArray()

    fun selectTop(orderNumber: Long) {
        top = nonDirtyItems.minByOrNull { Math.abs(it.orderNumber - orderNumber) }
    }

    fun updateItems(listOffset: Int, items: Array<ContinuousListItem>) {
        val currentItems = nonDirtyItems.filter { it.className == items[0].className }
        val foundOffset = currentItems.indexOf(items[0])
        if (foundOffset < 0) {
            listItems.addAll(items)
        } else if (foundOffset == listOffset) {
            // All good, we can go and combine things
            var allEqual = true
            val foundOffsetRemainder = currentItems.size - foundOffset
            for (i in 0 until Math.min(foundOffsetRemainder, items.size)) {
                if (currentItems[foundOffset + i] != items[i]) {
                    allEqual = false
                }
            }

            if (allEqual) {
                listItems.addAll(items.slice(foundOffsetRemainder until items.size))
            } else {
                // TODO
            }
            for (i in foundOffset + items.size until currentItems.size) {
                listItems.remove(currentItems[i])
            }
        } else {
            val listOffsetRemainder = currentItems.size - listOffset
            currentItems.slice(0 until listOffset).forEach { it.dirty = true }
            for (i in 0 until listOffsetRemainder) {
                listItems.remove(currentItems[listOffset + i])
            }
            items.slice(0 until items.size).forEach {
                listItems.add(it)
            }
        }
    }
}
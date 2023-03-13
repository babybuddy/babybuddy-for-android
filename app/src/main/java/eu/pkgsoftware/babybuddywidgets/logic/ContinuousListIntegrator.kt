package eu.pkgsoftware.babybuddywidgets.logic

class ContinuousListItem(val orderNumber: Long, val className: String, val id: String?) {
    var dirty = false

    override fun equals(other: Any?): Boolean {
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
        val currentItems = listItems.filter { it.className == items[0].className }
        val foundOffset = currentItems.indexOf(items[0])
        if (foundOffset < 0) {
            // We have nothing to go off, we need to trust the listOffset itself and pad everything with dummy values
            listItems.removeAll(currentItems)
            listItems.addAll((0 until listOffset).map {
                val dummy = ContinuousListItem(items[0].orderNumber - 1, items[0].className, null)
                dummy.dirty = true
                dummy
            })
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
            listItems.removeAll(
                currentItems.slice(foundOffset + items.size until currentItems.size)
            )
        } else {
            currentItems.slice(0 until listOffset).forEach { it.dirty = true }
            listItems.removeAll(currentItems.slice(listOffset until currentItems.size))
            listItems.addAll(items.slice(0 until items.size))
        }

        listItems.sortBy { it.orderNumber }
    }
}
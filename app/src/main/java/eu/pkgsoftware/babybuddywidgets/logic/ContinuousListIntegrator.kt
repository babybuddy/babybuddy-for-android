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
    private var topOffset = 0L
    private val listItems = mutableListOf<ContinuousListItem>()

    val items get() = listItems.toTypedArray()
    val nonDirtyItems get() = listItems.filter { !it.dirty }.toTypedArray()
    var top: ContinuousListItem?
        get() = nonDirtyItems.minByOrNull { Math.abs(it.orderNumber - topOffset) }
        set(v) {
            if (v == null) {
                selectTop(0L)
            } else {
                selectTop(v.orderNumber)
            }
        }

    fun selectTop(orderNumber: Long) {
        topOffset = orderNumber
    }

    fun updateItems(listOffset: Int, items: Array<ContinuousListItem>) {
        val currentItems = listItems.filter { it.className == items[0].className }
        val foundOffset = currentItems.indexOf(items[0])
        if (foundOffset < 0) {
            // We have nothing to go off, we need to trust the listOffset itself and pad everything with dummy values
            listItems.removeAll(currentItems)
            listItems.addAll((0 until listOffset).map {
                val dummy = ContinuousListItem(items[0].orderNumber, items[0].className, null)
                dummy.dirty = true
                dummy
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
            currentItems.slice(0 until listOffset).forEach { it.dirty = true }
            listItems.removeAll(currentItems.slice(listOffset until currentItems.size))
            listItems.addAll(items.slice(0 until items.size))
        }

        listItems.sortBy { it.orderNumber }
    }

    fun clear() {
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
}
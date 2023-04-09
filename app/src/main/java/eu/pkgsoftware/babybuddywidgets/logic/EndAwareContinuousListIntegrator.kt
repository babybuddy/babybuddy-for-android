package eu.pkgsoftware.babybuddywidgets.logic

class EndAwareContinuousListIntegrator : ContinuousListIntegrator() {
    private val itemCounts = mutableMapOf<String, Int>()

    fun updateItemsWithCount(listOffset: Int, totalCount: Int, className: String, items: Array<ContinuousListItem>) {
        itemCounts.put(className, totalCount)
        super.updateItems(listOffset, className, items)
    }

    override fun updateItems(listOffset: Int, className: String, items: Array<ContinuousListItem>) {
        itemCounts.remove(className)
        super.updateItems(listOffset, className, items)
    }

    override fun clear() {
        itemCounts.clear()
        super.clear()
    }

    fun computeValidCount(): Int {
        val allItems = super.items
        val itemsByClass = mutableMapOf<String, MutableList<ContinuousListItem>>()
        for (item in allItems) {
            val l = itemsByClass.computeIfAbsent(item.className) {
                mutableListOf()
            }
            l.add(item)
        }

        var validCount = allItems.size
        for ((className, count) in itemCounts) {
            if (count == 0) {
                continue
            }
            val items = itemsByClass[className]
            if (items == null || items.isEmpty()) {
                return 0
            }
            if (count == items.size) {
                continue // Complete list is not contributing to invalid tail
            }
            val lastValidClassItem = allItems.indexOf(items[Math.min(count, items.size) - 1])
            validCount = Math.min(validCount, lastValidClassItem + 1)
        }
        return validCount
    }
}
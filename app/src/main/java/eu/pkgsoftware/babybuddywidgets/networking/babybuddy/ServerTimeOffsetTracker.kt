package eu.pkgsoftware.babybuddywidgets.networking.babybuddy

import eu.pkgsoftware.babybuddywidgets.Constants

val MAX_OFFSETS = 20

open class ServerTimeOffsetTracker(initialOffsets: Sequence<Long> = sequenceOf()) {
    private var _offsets = initialOffsets.toMutableList()

    val offsets: List<Long> get() = _offsets.toList()
    val measuredOffset: Long get() = _offsets.let {
        if (it.size < 3) {
            -1000
        } else {
            val sortedOffsets = it.sorted()
            val p50 = sortedOffsets[it.size / 2]
            val p20 = sortedOffsets[it.size * 2 / 10]
            return p50 + (p20 - p50) * 5 / 3
        }
    }

    protected open fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    fun addOffsets(offsets: Sequence<Long>) {
        _offsets.addAll(offsets)
        while (_offsets.size > eu.pkgsoftware.babybuddywidgets.networking.babybuddy.MAX_OFFSETS) {
            _offsets.removeAt(0)
        }
    }

    fun updateServerTime(dateHeader: String) {
        val date = Constants.SERVER_DATE_FORMAT.parse(dateHeader)
        val serverMillis = date?.time ?: return

        // Note: System.currentTimeMillis() includes the connection latency, but including it
        // makes the offset larger, which does not hurt the reason for the offset. If latency is
        // high, we just assume that the server time is earlier by the same amount. In the worst
        // case, we log entries as being earlier than they actually are, as dictated by the current
        // connection latency. What we cannot have is a time arriving at the server that is later
        // than local server time.
        val newOffset = serverMillis - currentTimeMillis()
        addOffsets(sequenceOf(newOffset))
    }

    fun localToSafeServerTime(millis: Long): Long {
        val mOffset = measuredOffset
        val nowOffset = millis - currentTimeMillis()
        if (nowOffset < mOffset - 1000) {
            return millis
        }
        return millis + mOffset - 1000
    }

    fun serverToLocalTime(millis: Long): Long {
        return millis - measuredOffset
    }
}

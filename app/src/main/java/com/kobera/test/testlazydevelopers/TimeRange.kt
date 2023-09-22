package com.kobera.test.testlazydevelopers


import java.text.SimpleDateFormat
import java.util.Locale


open class Range<T: Comparable<T>>(val start: T, val end: T) {

    /** needle = x
     * (____x___)  -> true
     * (____)  x   -> false
     */
    fun contains(needle: T): Boolean {
        return needle >= start && needle <= end
    }

    /** other = [--]
     * (____[__)----] false
     * (_[----]_) true
     * [----]  (____) false
     */
    fun contains(other: Range<T>): Boolean {
        return contains(other.start) && contains(other.end)
    }


    /**
     * (____[__)----] true
     * (_[----]_) true
     * [--(____)--] true
     * [--(____]__) true
     * [----]  (____) false
     */
    fun overlaps(other: Range<T>): Boolean {
        return this.contains(other.start)
                || this.contains(other.end)
                || other.contains(this)
    }

    /** other = [--]
     * (____[__)----]   -> (__)
     * (___) [----]     -> throw
     * (_[----]_)       -> (____)
     * [-(_)--]         -> (_)
     */
    open fun trimBy(bounds: Range<T>): Range<T> {
        if (!overlaps(bounds)) {
            throw IllegalArgumentException("Cannot trim by non-overlapping range")
        }

        return Range(
            if (start < bounds.start) bounds.start else start,
            if (end > bounds.end) bounds.end else end
        )
    }
}


open class LongRange(start: Long, end: Long) : Range<Long>(start, end) {
    fun getSize(): Long {
        return end - start
    }
}


class TimeRange(start: Long, end: Long): LongRange(start, end) {
    fun getDuration() = getSize()

    fun trimBy(bounds: TimeRange): TimeRange =
        fromRange(trimBy(bounds as Range<Long>))

    override fun toString(): String {
        val dtf = SimpleDateFormat("EEEE yyyy-MM-dd HH:mm:ss.SSSS", Locale.getDefault())
        return "TimeRange{" +
                "start=" + dtf.format(start) +
                ", end=" + dtf.format(end) +
                '}'
    }

    companion object {
        fun fromPb(timeRange: TimeRange): TimeRange {
            return TimeRange(timeRange.start, timeRange.end)
        }

        private fun fromRange(range: Range<Long>): TimeRange {
            return TimeRange(range.start, range.end)
        }
    }
}

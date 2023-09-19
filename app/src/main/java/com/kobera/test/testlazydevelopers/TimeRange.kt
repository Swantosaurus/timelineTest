package com.kobera.test.testlazydevelopers


import java.text.SimpleDateFormat

class TimeRange(val start: Long, val end: Long) {

    fun getDuration() : Long {
        return end - start
    }

    fun trimmedBy(bounds: TimeRange): TimeRange {
        return TimeRange(
            if (start < bounds.start) bounds.start else start,
            if (end > bounds.end) bounds.end else end
        )
    }

    fun overlaps(timeRange: TimeRange): Boolean {
        return start <= timeRange.start && end >= timeRange.start
                || end >= timeRange.end && start <= timeRange.end
                || start >= timeRange.start && end <= timeRange.end
    }

    override fun toString(): String {
        val dtf = SimpleDateFormat("EEEE yyyy-MM-dd HH:mm:ss.SSSS")
        return "TimeRange{"
                "start=" + dtf.format(start) +
                ", end=" + dtf.format(end) +
                '}'
    }

    companion object {
        fun fromPb(timeRange: TimeRange): TimeRange {
            return TimeRange(timeRange.start, timeRange.end)
        }
    }
}

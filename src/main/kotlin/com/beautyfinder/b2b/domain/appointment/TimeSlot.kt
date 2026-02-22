package com.beautyfinder.b2b.domain.appointment

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

data class TimeSlot(val start: OffsetDateTime, val end: OffsetDateTime) {

    fun overlaps(other: TimeSlot): Boolean =
        start < other.end && end > other.start

    fun durationMinutes(): Long =
        ChronoUnit.MINUTES.between(start, end)
}

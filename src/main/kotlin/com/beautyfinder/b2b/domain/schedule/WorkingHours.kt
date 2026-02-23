package com.beautyfinder.b2b.domain.schedule

import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class WorkingHours(val start: LocalTime, val end: LocalTime) {

    init {
        require(end.isAfter(start)) { "End time must be after start time" }
    }

    fun containsSlot(slotStart: LocalTime, slotEnd: LocalTime): Boolean =
        !slotStart.isBefore(start) && !slotEnd.isAfter(end)

    fun durationMinutes(): Long =
        ChronoUnit.MINUTES.between(start, end)

    fun toMinuteRanges(): IntRange =
        (start.hour * 60 + start.minute)..(end.hour * 60 + end.minute)
}

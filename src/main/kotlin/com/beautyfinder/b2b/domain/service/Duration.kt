package com.beautyfinder.b2b.domain.service

import java.time.OffsetDateTime

data class Duration(val minutes: Int) {
    init {
        require(minutes in 5..480) { "Duration must be between 5 and 480 minutes" }
    }

    fun formatted(): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours == 0 -> "$mins min"
            mins == 0 -> "${hours}h"
            else -> "${hours}h $mins min"
        }
    }

    fun toEndTime(startAt: OffsetDateTime): OffsetDateTime = startAt.plusMinutes(minutes.toLong())
}

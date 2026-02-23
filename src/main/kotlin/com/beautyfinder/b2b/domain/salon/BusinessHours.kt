package com.beautyfinder.b2b.domain.salon

import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.time.ZoneId

data class BusinessHours(val openingHours: List<SalonOpeningHours>) {

    fun isOpenAt(dateTime: OffsetDateTime, timezone: ZoneId): Boolean {
        val localDateTime = dateTime.atZoneSameInstant(timezone).toLocalDateTime()
        val dayHours = getOpeningHoursForDay(localDateTime.dayOfWeek) ?: return false

        if (!dayHours.isOpen) return false

        val time = localDateTime.toLocalTime()
        val openTime = dayHours.openTime ?: return false
        val closeTime = dayHours.closeTime ?: return false

        if (time < openTime || time >= closeTime) return false

        if (dayHours.breakStart != null && dayHours.breakEnd != null) {
            if (time >= dayHours.breakStart && time < dayHours.breakEnd) return false
        }

        return true
    }

    fun getOpeningHoursForDay(dayOfWeek: DayOfWeek): SalonOpeningHours? =
        openingHours.find { it.dayOfWeek == dayOfWeek }

    fun nextOpeningTime(from: OffsetDateTime, timezone: ZoneId): OffsetDateTime? {
        val zoned = from.atZoneSameInstant(timezone)

        // Check if currently during break – return break end
        val todayHours = getOpeningHoursForDay(zoned.dayOfWeek)
        if (todayHours != null && todayHours.isOpen && todayHours.breakStart != null && todayHours.breakEnd != null) {
            val time = zoned.toLocalTime()
            if (time >= todayHours.breakStart && time < todayHours.breakEnd) {
                return zoned.withHour(todayHours.breakEnd.hour)
                    .withMinute(todayHours.breakEnd.minute)
                    .withSecond(0)
                    .withNano(0)
                    .toOffsetDateTime()
            }
        }

        // Check remaining days (up to 7)
        for (dayOffset in 0L..6L) {
            val candidate = zoned.plusDays(dayOffset)
            val dayHours = getOpeningHoursForDay(candidate.dayOfWeek) ?: continue
            if (!dayHours.isOpen || dayHours.openTime == null) continue

            val openDateTime = candidate.toLocalDate().atTime(dayHours.openTime).atZone(timezone)

            if (openDateTime.toOffsetDateTime() > from) {
                return openDateTime.toOffsetDateTime()
            }
        }

        return null
    }

    fun validate(): Result<Unit> {
        val days = openingHours.map { it.dayOfWeek }
        if (days.size != days.toSet().size) {
            return Result.failure(
                InvalidOpeningHoursException(
                    days.groupBy { it }.filter { it.value.size > 1 }.keys.first(),
                    "Duplicate day of week",
                ),
            )
        }

        for (hours in openingHours) {
            if (!hours.isOpen) continue

            if (hours.openTime == null || hours.closeTime == null) {
                return Result.failure(
                    InvalidOpeningHoursException(hours.dayOfWeek, "Open and close times are required when salon is open"),
                )
            }

            if (hours.openTime >= hours.closeTime) {
                return Result.failure(
                    InvalidOpeningHoursException(hours.dayOfWeek, "Open time must be before close time"),
                )
            }

            if (hours.breakStart != null && hours.breakEnd != null) {
                if (hours.breakStart >= hours.breakEnd) {
                    return Result.failure(
                        InvalidOpeningHoursException(hours.dayOfWeek, "Break start must be before break end"),
                    )
                }
                if (hours.breakStart < hours.openTime || hours.breakEnd > hours.closeTime) {
                    return Result.failure(
                        InvalidOpeningHoursException(hours.dayOfWeek, "Break must be within opening hours"),
                    )
                }
            }
        }

        return Result.success(Unit)
    }
}

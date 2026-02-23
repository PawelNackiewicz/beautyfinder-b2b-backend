package com.beautyfinder.b2b.domain.salon

import java.time.DayOfWeek
import java.util.UUID

open class SalonDomainException(message: String) : RuntimeException(message)

class SalonNotFoundException(id: UUID) :
    SalonDomainException("Salon not found: $id")

class SalonSlugAlreadyExistsException(slug: String) :
    SalonDomainException("Salon with slug '$slug' already exists")

class InvalidOpeningHoursException(val dayOfWeek: DayOfWeek, message: String) :
    SalonDomainException("Invalid opening hours for $dayOfWeek: $message")

class InvalidLoyaltyConfigException(val violations: List<String>) :
    SalonDomainException("Invalid loyalty configuration: ${violations.joinToString("; ")}")

class InvalidTimezoneException(timezone: String) :
    SalonDomainException("Invalid timezone: $timezone")

class InvalidCancellationWindowException(hours: Int) :
    SalonDomainException("Invalid cancellation window: $hours hours. Must be between 0 and 168.")

class InvalidSlotIntervalException(minutes: Int) :
    SalonDomainException("Invalid slot interval: $minutes minutes. Allowed values: 5, 10, 15, 20, 30, 60.")

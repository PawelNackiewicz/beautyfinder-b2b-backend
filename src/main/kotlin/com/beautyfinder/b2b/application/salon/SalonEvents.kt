package com.beautyfinder.b2b.application.salon

import java.util.UUID

data class SalonSettingsUpdatedEvent(val salonId: UUID, val changedFields: List<String>)

data class LoyaltyProgramEnabledEvent(val salonId: UUID)

data class SalonOpeningHoursUpdatedEvent(val salonId: UUID)

package com.beautyfinder.b2b.application.audit

import java.util.UUID

// Events published by other modules, consumed by AuditEventListener

data class SalonSettingsUpdatedEvent(val salonId: UUID, val actorId: UUID?, val changedSettings: List<String>)
data class LoyaltyProgramEnabledEvent(val salonId: UUID, val actorId: UUID?)
data class PasswordResetRequestedEvent(val salonId: UUID, val userId: UUID, val email: String)
data class PasswordChangedEvent(val salonId: UUID, val userId: UUID)
data class UserInvitedEvent(val salonId: UUID, val invitedEmail: String, val role: String, val invitedBy: UUID?)
data class UserActivatedEvent(val salonId: UUID, val userId: UUID, val email: String)
data class AccountLockedEvent(val salonId: UUID, val userId: UUID, val reason: String)

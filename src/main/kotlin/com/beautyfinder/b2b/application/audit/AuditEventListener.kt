package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditAction
import com.beautyfinder.b2b.domain.audit.AuditContext
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AuditEventListener(
    private val auditLogService: AuditLogService,
) {

    @EventListener
    @Async("auditExecutor")
    fun onSalonSettingsUpdated(event: SalonSettingsUpdatedEvent) {
        auditLogService.log(
            AuditLogEntry(
                salonId = event.salonId,
                action = AuditAction.SALON_SETTINGS_UPDATED,
                resourceType = "SALON",
                resourceId = event.salonId,
                metadata = mapOf("changedSettings" to event.changedSettings),
                context = event.actorId?.let { buildSystemContext(event.salonId, it) },
            ),
        )
    }

    @EventListener
    @Async("auditExecutor")
    fun onLoyaltyProgramEnabled(event: LoyaltyProgramEnabledEvent) {
        auditLogService.log(
            AuditLogEntry(
                salonId = event.salonId,
                action = AuditAction.LOYALTY_SETTINGS_UPDATED,
                resourceType = "SALON",
                resourceId = event.salonId,
                metadata = mapOf("loyaltyEnabled" to true),
                context = event.actorId?.let { buildSystemContext(event.salonId, it) },
            ),
        )
    }

    @EventListener
    @Async("auditExecutor")
    fun onPasswordResetRequested(event: PasswordResetRequestedEvent) {
        auditLogService.log(
            AuditLogEntry(
                salonId = event.salonId,
                action = AuditAction.PASSWORD_RESET,
                resourceType = "USER",
                resourceId = event.userId,
                resourceDescription = event.email,
            ),
        )
    }

    @EventListener
    @Async("auditExecutor")
    fun onPasswordChanged(event: PasswordChangedEvent) {
        auditLogService.log(
            AuditLogEntry(
                salonId = event.salonId,
                action = AuditAction.PASSWORD_CHANGED,
                resourceType = "USER",
                resourceId = event.userId,
            ),
        )
    }

    @EventListener
    @Async("auditExecutor")
    fun onUserInvited(event: UserInvitedEvent) {
        auditLogService.log(
            AuditLogEntry(
                salonId = event.salonId,
                action = AuditAction.USER_INVITED,
                resourceType = "USER",
                resourceDescription = event.invitedEmail,
                metadata = mapOf("role" to event.role),
                context = event.invitedBy?.let { buildSystemContext(event.salonId, it) },
            ),
        )
    }

    @EventListener
    @Async("auditExecutor")
    fun onUserActivated(event: UserActivatedEvent) {
        auditLogService.log(
            AuditLogEntry(
                salonId = event.salonId,
                action = AuditAction.USER_ACTIVATED,
                resourceType = "USER",
                resourceId = event.userId,
                resourceDescription = event.email,
            ),
        )
    }

    @EventListener
    @Async("auditExecutor")
    fun onAccountLocked(event: AccountLockedEvent) {
        auditLogService.log(
            AuditLogEntry(
                salonId = event.salonId,
                action = AuditAction.ACCOUNT_LOCKED,
                resourceType = "USER",
                resourceId = event.userId,
                metadata = mapOf("reason" to event.reason),
            ),
        )
    }

    private fun buildSystemContext(salonId: java.util.UUID, actorId: java.util.UUID) =
        AuditContext(
            salonId = salonId,
            actorId = actorId,
            actorEmail = null,
            actorRole = null,
            ipAddress = null,
            userAgent = null,
        )
}

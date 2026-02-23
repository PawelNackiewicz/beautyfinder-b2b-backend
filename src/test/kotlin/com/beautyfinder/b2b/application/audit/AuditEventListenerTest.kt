package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditAction
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class AuditEventListenerTest {

    private val auditLogService = mockk<AuditLogService>(relaxed = true)
    private val listener = AuditEventListener(auditLogService)

    private val salonId = UUID.randomUUID()

    @Test
    fun `onSalonSettingsUpdated logs correct action`() {
        listener.onSalonSettingsUpdated(
            SalonSettingsUpdatedEvent(salonId, UUID.randomUUID(), listOf("name", "phone")),
        )

        verify(exactly = 1) { auditLogService.log(any()) }
    }

    @Test
    fun `onSalonSettingsUpdated sets SALON resource type`() {
        val slot = slot<AuditLogEntry>()
        io.mockk.every { auditLogService.log(capture(slot)) } returns Unit

        listener.onSalonSettingsUpdated(
            SalonSettingsUpdatedEvent(salonId, UUID.randomUUID(), listOf("name")),
        )

        assertEquals(AuditAction.SALON_SETTINGS_UPDATED, slot.captured.action)
        assertEquals("SALON", slot.captured.resourceType)
        assertEquals(salonId, slot.captured.salonId)
    }

    @Test
    fun `onPasswordResetRequested logs with email description`() {
        val slot = slot<AuditLogEntry>()
        io.mockk.every { auditLogService.log(capture(slot)) } returns Unit

        val userId = UUID.randomUUID()
        listener.onPasswordResetRequested(
            PasswordResetRequestedEvent(salonId, userId, "user@test.com"),
        )

        assertEquals(AuditAction.PASSWORD_RESET, slot.captured.action)
        assertEquals("USER", slot.captured.resourceType)
        assertEquals(userId, slot.captured.resourceId)
        assertEquals("user@test.com", slot.captured.resourceDescription)
    }

    @Test
    fun `onUserInvited logs with role metadata`() {
        val slot = slot<AuditLogEntry>()
        io.mockk.every { auditLogService.log(capture(slot)) } returns Unit

        listener.onUserInvited(
            UserInvitedEvent(salonId, "new@test.com", "MANAGER", UUID.randomUUID()),
        )

        assertEquals(AuditAction.USER_INVITED, slot.captured.action)
        assertEquals("new@test.com", slot.captured.resourceDescription)
        assertEquals("MANAGER", slot.captured.metadata?.get("role"))
    }

    @Test
    fun `onAccountLocked logs with reason metadata`() {
        val slot = slot<AuditLogEntry>()
        io.mockk.every { auditLogService.log(capture(slot)) } returns Unit

        val userId = UUID.randomUUID()
        listener.onAccountLocked(
            AccountLockedEvent(salonId, userId, "too many failed attempts"),
        )

        assertEquals(AuditAction.ACCOUNT_LOCKED, slot.captured.action)
        assertEquals(userId, slot.captured.resourceId)
        assertEquals("too many failed attempts", slot.captured.metadata?.get("reason"))
    }
}

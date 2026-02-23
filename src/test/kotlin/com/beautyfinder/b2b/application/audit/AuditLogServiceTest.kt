package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditAction
import com.beautyfinder.b2b.domain.audit.AuditContext
import com.beautyfinder.b2b.domain.audit.AuditLog
import com.beautyfinder.b2b.domain.audit.AuditLogRetentionPolicy
import com.beautyfinder.b2b.infrastructure.audit.AuditLogRepository
import com.beautyfinder.b2b.infrastructure.audit.AuditLogRetentionPolicyRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import java.util.UUID

class AuditLogServiceTest {

    private val auditLogRepository = mockk<AuditLogRepository>(relaxed = true)
    private val retentionPolicyRepository = mockk<AuditLogRetentionPolicyRepository>(relaxed = true)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = AuditLogService(auditLogRepository, retentionPolicyRepository, objectMapper)

    private val salonId = UUID.randomUUID()

    @Test
    fun `log saves audit entry`() {
        val savedSlot = slot<AuditLog>()
        every { auditLogRepository.save(capture(savedSlot)) } answers { firstArg() }

        val entry = AuditLogEntry(
            salonId = salonId,
            action = AuditAction.CLIENT_CREATED,
            resourceType = "CLIENT",
            resourceId = UUID.randomUUID(),
        )

        service.log(entry)

        verify(exactly = 1) { auditLogRepository.save(any()) }
        assertEquals(AuditAction.CLIENT_CREATED, savedSlot.captured.action)
        assertEquals("CLIENT", savedSlot.captured.resourceType)
    }

    @Test
    fun `log repository throws does not propagate`() {
        every { auditLogRepository.save(any()) } throws RuntimeException("DB error")

        val entry = AuditLogEntry(
            salonId = salonId,
            action = AuditAction.CLIENT_CREATED,
            resourceType = "CLIENT",
        )

        // Should not throw
        service.log(entry)
    }

    @Test
    fun `log with context sets actor data`() {
        val savedSlot = slot<AuditLog>()
        every { auditLogRepository.save(capture(savedSlot)) } answers { firstArg() }

        val actorId = UUID.randomUUID()
        val ctx = AuditContext(
            salonId = salonId,
            actorId = actorId,
            actorEmail = "test@test.com",
            actorRole = "OWNER",
            ipAddress = "1.2.3.4",
            userAgent = "TestAgent",
        )

        val entry = AuditLogEntry(
            salonId = salonId,
            action = AuditAction.CLIENT_UPDATED,
            resourceType = "CLIENT",
            context = ctx,
        )

        service.log(entry)

        assertEquals(actorId, savedSlot.captured.actorId)
        assertEquals("test@test.com", savedSlot.captured.actorEmail)
        assertEquals("OWNER", savedSlot.captured.actorRole)
        assertEquals("1.2.3.4", savedSlot.captured.ipAddress)
    }

    @Test
    fun `log system action null actor`() {
        val savedSlot = slot<AuditLog>()
        every { auditLogRepository.save(capture(savedSlot)) } answers { firstArg() }

        val entry = AuditLogEntry(
            salonId = salonId,
            action = AuditAction.SYSTEM_AUTO_COMPLETE,
            resourceType = "SYSTEM",
            context = null,
        )

        service.log(entry)

        assertNull(savedSlot.captured.actorId)
    }

    @Test
    fun `searchLogs delegates to repository`() {
        every { auditLogRepository.search(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            PageImpl(emptyList())

        val query = AuditSearchQuery(action = AuditAction.LOGIN_SUCCESS)
        service.searchLogs(query, salonId, PageRequest.of(0, 20))

        verify(exactly = 1) {
            auditLogRepository.search(
                salonId = salonId,
                actorId = null,
                action = AuditAction.LOGIN_SUCCESS,
                resourceType = null,
                resourceId = null,
                from = null,
                to = null,
                pageable = any(),
            )
        }
    }

    @Test
    fun `getLogsForResource limits to 200`() {
        val resourceId = UUID.randomUUID()
        every {
            auditLogRepository.findAllByResourceTypeAndResourceIdAndSalonIdOrderByCreatedAtDesc(
                any(), any(), any(), any(),
            )
        } returns PageImpl(emptyList())

        service.getLogsForResource("CLIENT", resourceId, salonId)

        verify {
            auditLogRepository.findAllByResourceTypeAndResourceIdAndSalonIdOrderByCreatedAtDesc(
                "CLIENT", resourceId, salonId, PageRequest.of(0, 200),
            )
        }
    }

    @Test
    fun `getSensitiveDataAccessLog filters correct actions`() {
        val clientId = UUID.randomUUID()
        every {
            auditLogRepository.findAllByResourceTypeAndResourceIdAndSalonIdAndActionIn(
                any(), any(), any(), any(),
            )
        } returns emptyList()

        service.getSensitiveDataAccessLog(clientId, salonId)

        verify {
            auditLogRepository.findAllByResourceTypeAndResourceIdAndSalonIdAndActionIn(
                "CLIENT",
                clientId,
                salonId,
                listOf(AuditAction.CLIENT_SENSITIVE_DATA_ACCESSED, AuditAction.CLIENT_SENSITIVE_DATA_UPDATED),
            )
        }
    }

    @Test
    fun `purgeExpiredLogs calls both delete methods`() {
        val policy = AuditLogRetentionPolicy(salonId = salonId, retentionDays = 365, sensitiveDataRetentionDays = 1825)
        setId(policy, UUID.randomUUID())
        every { retentionPolicyRepository.findAll() } returns listOf(policy)
        every { auditLogRepository.deleteExpiredRegularLogs(any(), any(), any()) } returns 5
        every { auditLogRepository.deleteExpiredSensitiveLogs(any(), any(), any()) } returns 2

        service.purgeExpiredLogs()

        verify(exactly = 1) { auditLogRepository.deleteExpiredRegularLogs(salonId, any(), any()) }
        verify(exactly = 1) { auditLogRepository.deleteExpiredSensitiveLogs(salonId, any(), any()) }
    }

    @Test
    fun `purgeExpiredLogs logs deleted count`() {
        val policy = AuditLogRetentionPolicy(salonId = salonId)
        setId(policy, UUID.randomUUID())
        every { retentionPolicyRepository.findAll() } returns listOf(policy)
        every { auditLogRepository.deleteExpiredRegularLogs(any(), any(), any()) } returns 10
        every { auditLogRepository.deleteExpiredSensitiveLogs(any(), any(), any()) } returns 0

        service.purgeExpiredLogs()

        verify(exactly = 1) { auditLogRepository.deleteExpiredRegularLogs(any(), any(), any()) }
    }

    private fun setId(entity: com.beautyfinder.b2b.domain.BaseEntity, id: UUID) {
        val field = com.beautyfinder.b2b.domain.BaseEntity::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(entity, id)
    }
}

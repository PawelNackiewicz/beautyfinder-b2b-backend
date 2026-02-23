package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditAction
import com.beautyfinder.b2b.domain.audit.AuditLog
import com.beautyfinder.b2b.infrastructure.audit.AuditLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@Testcontainers
class AuditLogIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Test
    fun `save and retrieve audit log`() {
        val salonId = UUID.randomUUID()
        val log = AuditLog(
            salonId = salonId,
            actorId = UUID.randomUUID(),
            actorEmail = "test@example.com",
            actorRole = "OWNER",
            action = AuditAction.CLIENT_CREATED,
            resourceType = "CLIENT",
            resourceId = UUID.randomUUID(),
            resourceDescription = null,
            metadata = """{"key":"value"}""",
            changedFields = null,
            ipAddress = "127.0.0.1",
            userAgent = "TestAgent",
        )

        val saved = auditLogRepository.save(log)
        assertNotNull(saved.id)
        assertEquals(AuditAction.CLIENT_CREATED, saved.action)
    }

    @Test
    fun `search with filters`() {
        val salonId = UUID.randomUUID()
        val actorId = UUID.randomUUID()

        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = actorId, actorEmail = null,
                actorRole = "OWNER", action = AuditAction.LOGIN_SUCCESS,
                resourceType = "USER", resourceId = null, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
            ),
        )
        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = actorId, actorEmail = null,
                actorRole = "OWNER", action = AuditAction.CLIENT_CREATED,
                resourceType = "CLIENT", resourceId = null, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
            ),
        )

        val results = auditLogRepository.search(
            salonId = salonId,
            actorId = null,
            action = AuditAction.LOGIN_SUCCESS,
            resourceType = null,
            resourceId = null,
            from = null,
            to = null,
            pageable = PageRequest.of(0, 10),
        )

        assertEquals(1, results.totalElements)
        assertEquals(AuditAction.LOGIN_SUCCESS, results.content[0].action)
    }

    @Test
    fun `search by date range`() {
        val salonId = UUID.randomUUID()

        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = null, actorEmail = null,
                actorRole = null, action = AuditAction.SYSTEM_AUTO_COMPLETE,
                resourceType = "SYSTEM", resourceId = null, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
                createdAt = OffsetDateTime.now().minusDays(10),
            ),
        )
        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = null, actorEmail = null,
                actorRole = null, action = AuditAction.SYSTEM_AUTO_COMPLETE,
                resourceType = "SYSTEM", resourceId = null, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
                createdAt = OffsetDateTime.now(),
            ),
        )

        val results = auditLogRepository.search(
            salonId = salonId,
            actorId = null,
            action = null,
            resourceType = null,
            resourceId = null,
            from = OffsetDateTime.now().minusDays(5),
            to = null,
            pageable = PageRequest.of(0, 10),
        )

        assertEquals(1, results.totalElements)
    }

    @Test
    fun `findAllByResourceTypeAndResourceId returns correct logs`() {
        val salonId = UUID.randomUUID()
        val resourceId = UUID.randomUUID()

        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = UUID.randomUUID(), actorEmail = null,
                actorRole = "OWNER", action = AuditAction.CLIENT_UPDATED,
                resourceType = "CLIENT", resourceId = resourceId, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
            ),
        )
        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = UUID.randomUUID(), actorEmail = null,
                actorRole = "OWNER", action = AuditAction.CLIENT_CREATED,
                resourceType = "CLIENT", resourceId = resourceId, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
            ),
        )

        val results = auditLogRepository.findAllByResourceTypeAndResourceIdAndSalonIdOrderByCreatedAtDesc(
            "CLIENT", resourceId, salonId, PageRequest.of(0, 200),
        )

        assertEquals(2, results.totalElements)
    }

    @Test
    fun `delete expired logs`() {
        val salonId = UUID.randomUUID()
        val sensitiveActions = listOf(AuditAction.CLIENT_SENSITIVE_DATA_ACCESSED, AuditAction.CLIENT_SENSITIVE_DATA_UPDATED)

        // Old regular log
        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = null, actorEmail = null,
                actorRole = null, action = AuditAction.LOGIN_SUCCESS,
                resourceType = "USER", resourceId = null, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
                createdAt = OffsetDateTime.now().minusDays(400),
            ),
        )
        // Recent regular log
        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = null, actorEmail = null,
                actorRole = null, action = AuditAction.LOGIN_SUCCESS,
                resourceType = "USER", resourceId = null, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
            ),
        )

        val deleted = auditLogRepository.deleteExpiredRegularLogs(
            salonId, OffsetDateTime.now().minusDays(365), sensitiveActions,
        )

        assertEquals(1, deleted)
    }

    @Test
    fun `sensitive data access log query`() {
        val salonId = UUID.randomUUID()
        val clientId = UUID.randomUUID()

        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = UUID.randomUUID(), actorEmail = "owner@test.com",
                actorRole = "OWNER", action = AuditAction.CLIENT_SENSITIVE_DATA_ACCESSED,
                resourceType = "CLIENT", resourceId = clientId, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = "1.2.3.4", userAgent = null,
            ),
        )
        // Non-sensitive log for same client
        auditLogRepository.save(
            AuditLog(
                salonId = salonId, actorId = UUID.randomUUID(), actorEmail = null,
                actorRole = "OWNER", action = AuditAction.CLIENT_UPDATED,
                resourceType = "CLIENT", resourceId = clientId, resourceDescription = null,
                metadata = null, changedFields = null, ipAddress = null, userAgent = null,
            ),
        )

        val results = auditLogRepository.findAllByResourceTypeAndResourceIdAndSalonIdAndActionIn(
            "CLIENT", clientId, salonId,
            listOf(AuditAction.CLIENT_SENSITIVE_DATA_ACCESSED, AuditAction.CLIENT_SENSITIVE_DATA_UPDATED),
        )

        assertEquals(1, results.size)
        assertEquals(AuditAction.CLIENT_SENSITIVE_DATA_ACCESSED, results[0].action)
    }
}

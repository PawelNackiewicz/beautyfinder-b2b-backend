package com.beautyfinder.b2b.api.audit

import com.beautyfinder.b2b.application.audit.ActorActivityDto
import com.beautyfinder.b2b.application.audit.AuditLogDto
import com.beautyfinder.b2b.application.audit.AuditLogService
import com.beautyfinder.b2b.application.audit.AuditSummaryDto
import com.beautyfinder.b2b.config.JwtAuthenticationFilter
import com.beautyfinder.b2b.config.JwtService
import com.beautyfinder.b2b.config.SecurityConfig
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.audit.AuditAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.OffsetDateTime
import java.util.UUID

@WebMvcTest(AuditLogController::class)
@Import(AuditLogControllerTest.MockConfig::class, SecurityConfig::class)
class AuditLogControllerTest {

    @TestConfiguration
    class MockConfig {
        @Bean
        fun auditLogService(): AuditLogService = mockk(relaxed = true)

        @Bean
        fun jwtService(): JwtService = mockk()

        @Bean
        fun jwtAuthenticationFilter(jwtService: JwtService): JwtAuthenticationFilter =
            JwtAuthenticationFilter(jwtService)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var auditLogService: AuditLogService

    private val salonId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mockkObject(TenantContext)
        every { TenantContext.getSalonId() } returns salonId
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TenantContext)
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getLogs returns 200 for owner`() {
        every { auditLogService.searchLogs(any(), any(), any()) } returns PageImpl(emptyList())

        mockMvc.get("/api/audit/logs").andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["EMPLOYEE"])
    fun `getLogs returns 403 for non-owner`() {
        mockMvc.get("/api/audit/logs").andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `getLogs returns 403 for unauthenticated`() {
        mockMvc.get("/api/audit/logs").andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getResourceHistory returns 200`() {
        every { auditLogService.getLogsForResource(any(), any(), any()) } returns emptyList()

        val resourceId = UUID.randomUUID()
        mockMvc.get("/api/audit/logs/resource/CLIENT/$resourceId").andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getActorHistory returns 200`() {
        every { auditLogService.getLogsForActor(any(), any(), any()) } returns PageImpl(emptyList())

        val actorId = UUID.randomUUID()
        mockMvc.get("/api/audit/logs/actor/$actorId").andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getSensitiveAccessLog returns 200`() {
        every { auditLogService.getSensitiveDataAccessLog(any(), any()) } returns emptyList()

        val clientId = UUID.randomUUID()
        mockMvc.get("/api/audit/logs/sensitive-access/$clientId").andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getSummary returns 200`() {
        every { auditLogService.getSummary(any(), any()) } returns AuditSummaryDto(
            totalActions = 100,
            actionBreakdown = mapOf("LOGIN_SUCCESS" to 50L),
            mostActiveActors = emptyList(),
            sensitiveDataAccesses = 5,
            period = "LAST_30_DAYS",
        )

        mockMvc.get("/api/audit/summary").andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `getSummary returns 403 for manager`() {
        mockMvc.get("/api/audit/summary").andExpect {
            status { isForbidden() }
        }
    }
}

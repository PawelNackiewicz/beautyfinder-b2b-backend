package com.beautyfinder.b2b.api.appointment

import com.beautyfinder.b2b.application.appointment.AppointmentDto
import com.beautyfinder.b2b.application.appointment.AppointmentService
import com.beautyfinder.b2b.config.JwtAuthenticationFilter
import com.beautyfinder.b2b.config.JwtService
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.appointment.AppointmentConflictException
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.appointment.InvalidStatusTransitionException
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
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
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@WebMvcTest(AppointmentController::class)
@Import(AppointmentControllerTest.MockConfig::class, com.beautyfinder.b2b.config.SecurityConfig::class)
class AppointmentControllerTest {

    @TestConfiguration
    class MockConfig {
        @Bean
        fun appointmentService(): AppointmentService = io.mockk.mockk()

        @Bean
        fun jwtService(): JwtService = io.mockk.mockk()

        @Bean
        fun jwtAuthenticationFilter(jwtService: JwtService): JwtAuthenticationFilter =
            JwtAuthenticationFilter(jwtService)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var appointmentService: AppointmentService

    private val salonId = UUID.randomUUID()

    private fun buildDto(
        id: UUID = UUID.randomUUID(),
        status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    ) = AppointmentDto(
        id = id,
        salonId = salonId,
        clientId = UUID.randomUUID(),
        clientName = "Jane Doe",
        employeeId = UUID.randomUUID(),
        employeeName = "John",
        variantId = UUID.randomUUID(),
        variantName = "Standard",
        serviceName = "Haircut",
        startAt = OffsetDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC),
        endAt = OffsetDateTime.of(2025, 6, 15, 11, 0, 0, 0, ZoneOffset.UTC),
        status = status,
        source = AppointmentSource.DIRECT,
        finalPrice = BigDecimal("100.00"),
        commissionValue = null,
        notes = null,
        cancellationReason = null,
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
    )

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
    @WithMockUser(roles = ["MANAGER"])
    fun `getAppointments returns list with 200`() {
        // given
        every { appointmentService.getAppointments(any(), eq(salonId)) } returns listOf(buildDto())

        // when/then
        mockMvc.get("/api/appointments?date=2025-06-15")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].clientName") { value("Jane Doe") }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `getAppointments missing date param returns 400`() {
        mockMvc.get("/api/appointments")
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createAppointment with valid request returns 201`() {
        // given
        every { appointmentService.createAppointment(any(), eq(salonId)) } returns buildDto()

        val request = mapOf(
            "clientId" to UUID.randomUUID(),
            "employeeId" to UUID.randomUUID(),
            "variantId" to UUID.randomUUID(),
            "startAt" to "2099-12-15T10:00:00Z",
            "source" to "DIRECT",
        )

        // when/then
        mockMvc.post("/api/appointments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createAppointment missing clientId returns 400`() {
        val request = mapOf(
            "employeeId" to UUID.randomUUID(),
            "variantId" to UUID.randomUUID(),
            "startAt" to "2099-12-15T10:00:00Z",
        )

        mockMvc.post("/api/appointments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `updateStatus with valid body returns 200`() {
        // given
        val appointmentId = UUID.randomUUID()
        every {
            appointmentService.updateAppointmentStatus(appointmentId, AppointmentStatus.CONFIRMED, null, salonId)
        } returns buildDto(id = appointmentId, status = AppointmentStatus.CONFIRMED)

        val request = mapOf("newStatus" to "CONFIRMED")

        // when/then
        mockMvc.patch("/api/appointments/$appointmentId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `updateStatus conflict exception returns 409`() {
        // given
        val appointmentId = UUID.randomUUID()
        every {
            appointmentService.updateAppointmentStatus(any(), any(), any(), any())
        } throws AppointmentConflictException(UUID.randomUUID(), OffsetDateTime.now())

        val request = mapOf("newStatus" to "CONFIRMED")

        // when/then
        mockMvc.patch("/api/appointments/$appointmentId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `updateStatus invalid transition returns 422`() {
        // given
        val appointmentId = UUID.randomUUID()
        every {
            appointmentService.updateAppointmentStatus(any(), any(), any(), any())
        } throws InvalidStatusTransitionException(AppointmentStatus.COMPLETED, AppointmentStatus.SCHEDULED)

        val request = mapOf("newStatus" to "SCHEDULED")

        // when/then
        mockMvc.patch("/api/appointments/$appointmentId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnprocessableEntity() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `reschedule returns 200`() {
        // given
        val appointmentId = UUID.randomUUID()
        every {
            appointmentService.rescheduleAppointment(appointmentId, any(), salonId)
        } returns buildDto(id = appointmentId)

        val request = mapOf("newStartAt" to "2099-12-20T14:00:00Z")

        // when/then
        mockMvc.patch("/api/appointments/$appointmentId/reschedule") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }
    }
}

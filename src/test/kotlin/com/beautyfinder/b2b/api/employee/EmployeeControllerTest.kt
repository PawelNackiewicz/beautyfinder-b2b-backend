package com.beautyfinder.b2b.api.employee

import com.beautyfinder.b2b.application.employee.AvailableSlotDto
import com.beautyfinder.b2b.application.employee.EmployeeDto
import com.beautyfinder.b2b.application.employee.EmployeeService
import com.beautyfinder.b2b.application.employee.ScheduleExceptionDto
import com.beautyfinder.b2b.application.employee.WeeklyScheduleDto
import com.beautyfinder.b2b.config.JwtAuthenticationFilter
import com.beautyfinder.b2b.config.JwtService
import com.beautyfinder.b2b.config.SecurityConfig
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.employee.CannotDeleteActiveEmployeeException
import com.beautyfinder.b2b.domain.employee.EmployeeNotFoundException
import com.beautyfinder.b2b.domain.employee.EmployeeStatus
import com.beautyfinder.b2b.domain.employee.InvalidScheduleException
import com.beautyfinder.b2b.domain.employee.ScheduleExceptionOverlapException
import com.beautyfinder.b2b.domain.schedule.ScheduleExceptionType
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.justRun
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@WebMvcTest(EmployeeController::class)
@Import(EmployeeControllerTest.MockConfig::class, SecurityConfig::class)
class EmployeeControllerTest {

    @TestConfiguration
    class MockConfig {
        @Bean
        fun employeeService(): EmployeeService = io.mockk.mockk()

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
    private lateinit var employeeService: EmployeeService

    private val salonId = UUID.randomUUID()

    private fun buildDto(id: UUID = UUID.randomUUID()) = EmployeeDto(
        id = id,
        salonId = salonId,
        userId = UUID.randomUUID(),
        displayName = "John",
        phone = null,
        avatarUrl = null,
        color = null,
        status = EmployeeStatus.ACTIVE,
        serviceIds = emptySet(),
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
    fun `getEmployees returns 200 with list`() {
        every { employeeService.listEmployees(salonId, false) } returns listOf(buildDto())

        mockMvc.get("/api/employees")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].displayName") { value("John") }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `getEmployee not found returns 404`() {
        val id = UUID.randomUUID()
        every { employeeService.getEmployee(id, salonId) } throws EmployeeNotFoundException(id)

        mockMvc.get("/api/employees/$id")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `createEmployee valid request returns 201`() {
        every { employeeService.createEmployee(any(), eq(salonId)) } returns buildDto()

        val request = mapOf(
            "userId" to UUID.randomUUID(),
            "displayName" to "John",
        )

        mockMvc.post("/api/employees") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `createEmployee invalid color returns 400`() {
        val request = mapOf(
            "userId" to UUID.randomUUID(),
            "displayName" to "John",
            "color" to "#ZZZ",
        )

        mockMvc.post("/api/employees") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `createEmployee invalid phone returns 400`() {
        val request = mapOf(
            "userId" to UUID.randomUUID(),
            "displayName" to "John",
            "phone" to "not-a-phone",
        )

        mockMvc.post("/api/employees") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `updateEmployee returns 200`() {
        val id = UUID.randomUUID()
        every { employeeService.updateEmployee(id, any(), salonId) } returns buildDto(id)

        val request = mapOf("displayName" to "Jane")

        mockMvc.put("/api/employees/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `deactivateEmployee has future appointments returns 422`() {
        val id = UUID.randomUUID()
        every { employeeService.deactivateEmployee(id, salonId) } throws CannotDeleteActiveEmployeeException(id, 3)

        mockMvc.delete("/api/employees/$id")
            .andExpect {
                status { isUnprocessableEntity() }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `upsertSchedule valid returns 200`() {
        val id = UUID.randomUUID()
        every { employeeService.upsertWeeklySchedule(id, any(), salonId) } returns
            DayOfWeek.entries.map {
                WeeklyScheduleDto(null, id, it, LocalTime.of(0, 0), LocalTime.of(0, 0), false)
            }

        val schedules = listOf(
            mapOf("dayOfWeek" to "MONDAY", "isWorkingDay" to true, "startTime" to "09:00", "endTime" to "17:00"),
        )

        mockMvc.put("/api/employees/$id/schedule") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(schedules)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `addException valid returns 201`() {
        val id = UUID.randomUUID()
        every { employeeService.addScheduleException(id, any(), salonId) } returns ScheduleExceptionDto(
            id = UUID.randomUUID(),
            employeeId = id,
            startAt = OffsetDateTime.now().plusDays(5),
            endAt = OffsetDateTime.now().plusDays(6),
            reason = "Vacation",
            type = ScheduleExceptionType.VACATION,
            createdAt = OffsetDateTime.now(),
        )

        val request = mapOf(
            "startAt" to "2099-06-15T09:00:00Z",
            "endAt" to "2099-06-16T17:00:00Z",
            "reason" to "Vacation",
            "type" to "VACATION",
        )

        mockMvc.post("/api/employees/$id/exceptions") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `addException overlap returns 409`() {
        val id = UUID.randomUUID()
        every { employeeService.addScheduleException(id, any(), salonId) } throws
            ScheduleExceptionOverlapException(id, OffsetDateTime.now(), OffsetDateTime.now().plusDays(1))

        val request = mapOf(
            "startAt" to "2099-06-15T09:00:00Z",
            "endAt" to "2099-06-16T17:00:00Z",
            "type" to "VACATION",
        )

        mockMvc.post("/api/employees/$id/exceptions") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `deleteException returns 204`() {
        val id = UUID.randomUUID()
        val exceptionId = UUID.randomUUID()
        justRun { employeeService.deleteScheduleException(exceptionId, id, salonId) }

        mockMvc.delete("/api/employees/$id/exceptions/$exceptionId")
            .andExpect {
                status { isNoContent() }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `getAvailableSlots returns 200 with slots`() {
        val id = UUID.randomUUID()
        every { employeeService.getAvailableSlots(id, any(), 60, salonId) } returns listOf(
            AvailableSlotDto(
                start = OffsetDateTime.of(2099, 6, 16, 9, 0, 0, 0, ZoneOffset.UTC),
                end = OffsetDateTime.of(2099, 6, 16, 10, 0, 0, 0, ZoneOffset.UTC),
                durationMinutes = 60,
            ),
        )

        mockMvc.get("/api/employees/$id/available-slots?date=2099-06-16&variantDurationMinutes=60")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].durationMinutes") { value(60) }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `getAvailableSlots invalid duration returns 400`() {
        mockMvc.get("/api/employees/${UUID.randomUUID()}/available-slots?date=2099-06-16&variantDurationMinutes=5")
            .andExpect {
                status { isBadRequest() }
            }
    }
}

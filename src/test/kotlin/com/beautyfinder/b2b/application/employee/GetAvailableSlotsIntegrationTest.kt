package com.beautyfinder.b2b.application.employee

import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.service.Service
import com.beautyfinder.b2b.domain.service.ServiceVariant
import com.beautyfinder.b2b.domain.User
import com.beautyfinder.b2b.domain.UserRole
import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.employee.EmployeeStatus
import com.beautyfinder.b2b.domain.schedule.ScheduleException
import com.beautyfinder.b2b.domain.schedule.ScheduleExceptionType
import com.beautyfinder.b2b.domain.schedule.WeeklySchedule
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@Testcontainers
@Transactional
class GetAvailableSlotsIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var salon: Salon
    private lateinit var employee: Employee
    private lateinit var client: Client
    private lateinit var variant: ServiceVariant

    // Use a future Wednesday (2099-06-16 is a Wednesday)
    private val wednesday = LocalDate.of(2099, 6, 16)
    private val saturday = LocalDate.of(2099, 6, 19)

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Test Salon", slug = "test-${UUID.randomUUID()}")
        entityManager.persist(salon)

        val user = User(salonId = salon.id!!, role = UserRole.EMPLOYEE, email = "e-${UUID.randomUUID()}@test.com", passwordHash = "h")
        entityManager.persist(user)

        employee = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "John", status = EmployeeStatus.ACTIVE)
        entityManager.persist(employee)

        val service = Service(salonId = salon.id!!, name = "Haircut", category = "Hair")
        entityManager.persist(service)

        variant = ServiceVariant(serviceId = service.id!!, salonId = salon.id!!, name = "Standard", durationMinutes = 60, price = BigDecimal("50.00"))
        entityManager.persist(variant)

        client = Client(salonId = salon.id!!, firstName = "Jane", lastName = "Doe", phone = "+48123456789")
        entityManager.persist(client)

        // Mon-Fri 9:00-17:00
        for (day in listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            val schedule = WeeklySchedule(
                employeeId = employee.id!!,
                salonId = salon.id!!,
                dayOfWeek = day,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                isWorkingDay = true,
            )
            entityManager.persist(schedule)
        }

        // Appointment on Wednesday 10:00-11:00
        val apptStart = wednesday.atTime(10, 0).atOffset(ZoneOffset.UTC)
        val apptEnd = wednesday.atTime(11, 0).atOffset(ZoneOffset.UTC)
        val appointment = Appointment(
            salonId = salon.id!!,
            clientId = client.id!!,
            employeeId = employee.id!!,
            variantId = variant.id!!,
            startAt = apptStart,
            endAt = apptEnd,
            status = AppointmentStatus.SCHEDULED,
            source = AppointmentSource.DIRECT,
            finalPrice = BigDecimal("50.00"),
        )
        entityManager.persist(appointment)

        // Schedule exception: Wednesday 14:00-15:00 BLOCKED
        val blockStart = wednesday.atTime(14, 0).atOffset(ZoneOffset.UTC)
        val blockEnd = wednesday.atTime(15, 0).atOffset(ZoneOffset.UTC)
        val scheduleException = ScheduleException(
            employeeId = employee.id!!,
            salonId = salon.id!!,
            startAt = blockStart,
            endAt = blockEnd,
            reason = "Personal",
            type = ScheduleExceptionType.BLOCKED,
        )
        entityManager.persist(scheduleException)

        entityManager.flush()
    }

    @Test
    fun `wednesday 60min slots - excludes booked and blocked times`() {
        // when
        val slots = employeeService.getAvailableSlots(employee.id!!, wednesday, 60, salon.id!!)

        // then
        // 9:00-10:00 should be available
        assertTrue(slots.any { it.start.toLocalTime() == LocalTime.of(9, 0) }, "9:00 slot should be available")

        // 10:00-10:45 slots should NOT be available (booked 10-11)
        assertTrue(slots.none { it.start.toLocalTime() == LocalTime.of(10, 0) }, "10:00 slot should not be available")
        assertTrue(slots.none { it.start.toLocalTime() == LocalTime.of(10, 15) }, "10:15 slot should not be available")
        assertTrue(slots.none { it.start.toLocalTime() == LocalTime.of(10, 30) }, "10:30 slot should not be available")

        // 11:00-12:00 should be available
        assertTrue(slots.any { it.start.toLocalTime() == LocalTime.of(11, 0) }, "11:00 slot should be available")

        // 13:00-14:00 should be available (before block)
        assertTrue(slots.any { it.start.toLocalTime() == LocalTime.of(13, 0) }, "13:00 slot should be available")

        // 14:00-14:45 slots should NOT be available (blocked 14-15)
        assertTrue(slots.none { it.start.toLocalTime() == LocalTime.of(14, 0) }, "14:00 slot should not be available")
    }

    @Test
    fun `saturday - no schedule returns empty`() {
        // when
        val slots = employeeService.getAvailableSlots(employee.id!!, saturday, 30, salon.id!!)

        // then
        assertTrue(slots.isEmpty(), "Saturday should have no available slots")
    }
}

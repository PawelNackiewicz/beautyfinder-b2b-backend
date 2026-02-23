package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.Service
import com.beautyfinder.b2b.domain.ServiceVariant
import com.beautyfinder.b2b.domain.User
import com.beautyfinder.b2b.domain.UserRole
import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AppointmentRepositoryTest {

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
    private lateinit var appointmentRepository: AppointmentRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var salon: Salon
    private lateinit var employee: Employee
    private lateinit var client: Client
    private lateinit var variant: ServiceVariant

    private val baseTime = OffsetDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Test Salon", slug = "test-salon-${UUID.randomUUID()}")
        entityManager.persist(salon)

        val user = User(salonId = salon.id!!, role = UserRole.OWNER, email = "test-${UUID.randomUUID()}@test.com", passwordHash = "hash")
        entityManager.persist(user)

        employee = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "John")
        entityManager.persist(employee)

        val service = Service(salonId = salon.id!!, name = "Haircut", category = "Hair")
        entityManager.persist(service)

        variant = ServiceVariant(serviceId = service.id!!, name = "Standard", durationMinutes = 60, price = BigDecimal("50.00"))
        entityManager.persist(variant)

        client = Client(salonId = salon.id!!, firstName = "Jane", lastName = "Doe", phone = "+48123456789")
        entityManager.persist(client)

        entityManager.flush()
    }

    private fun persistAppointment(
        startAt: OffsetDateTime = baseTime,
        endAt: OffsetDateTime = startAt.plusMinutes(60),
        status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    ): Appointment {
        val appointment = Appointment(
            salonId = salon.id!!,
            clientId = client.id!!,
            employeeId = employee.id!!,
            variantId = variant.id!!,
            startAt = startAt,
            endAt = endAt,
            status = status,
            finalPrice = BigDecimal("50.00"),
            source = AppointmentSource.DIRECT,
        )
        entityManager.persist(appointment)
        entityManager.flush()
        return appointment
    }

    @Test
    fun `findAllBySalonIdAndStartAtBetween returns correct appointments`() {
        // given
        val appointment = persistAppointment()

        val from = baseTime.minusHours(1)
        val to = baseTime.plusHours(2)

        // when
        val results = appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salon.id!!, from, to)

        // then
        assertEquals(1, results.size)
        assertEquals(appointment.id, results[0].id)
    }

    @Test
    fun `findConflictingAppointments - overlapping slot returns conflict`() {
        // given
        val existing = persistAppointment(startAt = baseTime, endAt = baseTime.plusMinutes(60))

        // when – new slot 10:30-11:30 overlaps with 10:00-11:00
        val conflicts = appointmentRepository.findConflictingAppointments(
            employeeId = employee.id!!,
            startAt = baseTime.plusMinutes(30),
            endAt = baseTime.plusMinutes(90),
            statuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED),
        )

        // then
        assertEquals(1, conflicts.size)
        assertEquals(existing.id, conflicts[0].id)
    }

    @Test
    fun `findConflictingAppointments - non-overlapping slot returns empty`() {
        // given
        persistAppointment(startAt = baseTime, endAt = baseTime.plusMinutes(60))

        // when – new slot 12:00-13:00, no overlap with 10:00-11:00
        val conflicts = appointmentRepository.findConflictingAppointments(
            employeeId = employee.id!!,
            startAt = baseTime.plusHours(2),
            endAt = baseTime.plusHours(3),
            statuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED),
        )

        // then
        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun `findConflictingAppointments with excludeId - excludes self`() {
        // given
        val appointment = persistAppointment()

        // when – same time but exclude self (reschedule case)
        val conflicts = appointmentRepository.findConflictingAppointments(
            employeeId = employee.id!!,
            startAt = baseTime,
            endAt = baseTime.plusMinutes(60),
            statuses = listOf(AppointmentStatus.SCHEDULED),
            excludeId = appointment.id!!,
        )

        // then
        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun `findAllByStatusInAndEndAtBefore returns only past appointments`() {
        // given
        val past = persistAppointment(
            startAt = baseTime.minusHours(3),
            endAt = baseTime.minusHours(2),
            status = AppointmentStatus.SCHEDULED,
        )
        persistAppointment(
            startAt = baseTime.plusHours(5),
            endAt = baseTime.plusHours(6),
            status = AppointmentStatus.SCHEDULED,
        )

        // when
        val results = appointmentRepository.findAllByStatusInAndEndAtBefore(
            statuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS),
            endAt = baseTime,
        )

        // then
        assertEquals(1, results.size)
        assertEquals(past.id, results[0].id)
    }
}

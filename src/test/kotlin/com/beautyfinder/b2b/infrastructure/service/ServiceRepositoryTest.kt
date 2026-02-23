package com.beautyfinder.b2b.infrastructure.service

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.User
import com.beautyfinder.b2b.domain.UserRole
import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.service.Service
import com.beautyfinder.b2b.domain.service.ServiceStatus
import com.beautyfinder.b2b.domain.service.ServiceVariant
import com.beautyfinder.b2b.domain.service.VariantStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
class ServiceRepositoryTest {

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
    private lateinit var serviceRepository: ServiceRepository

    @Autowired
    private lateinit var variantRepository: ServiceVariantRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var salon: Salon
    private lateinit var salon2: Salon

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Salon 1", slug = "salon1-${UUID.randomUUID()}")
        entityManager.persist(salon)
        salon2 = Salon(name = "Salon 2", slug = "salon2-${UUID.randomUUID()}")
        entityManager.persist(salon2)
        entityManager.flush()
    }

    @Test
    fun `findAllBySalonIdAndStatusNot filters archived`() {
        val active = Service(salonId = salon.id!!, name = "Active", category = "Cat", status = ServiceStatus.ACTIVE)
        val archived = Service(salonId = salon.id!!, name = "Archived", category = "Cat", status = ServiceStatus.ARCHIVED)
        entityManager.persist(active)
        entityManager.persist(archived)
        entityManager.flush()

        val result = serviceRepository.findAllBySalonIdAndStatusNotOrderByDisplayOrderAsc(salon.id!!, ServiceStatus.ARCHIVED)
        assertEquals(1, result.size)
        assertEquals("Active", result[0].name)
    }

    @Test
    fun `existsByNameAndSalonId cross salon isolation`() {
        val service = Service(salonId = salon.id!!, name = "Haircut", category = "Hair")
        entityManager.persist(service)
        entityManager.flush()

        assertTrue(serviceRepository.existsByNameAndSalonId("Haircut", salon.id!!))
        assertFalse(serviceRepository.existsByNameAndSalonId("Haircut", salon2.id!!))
    }

    @Test
    fun `findMaxDisplayOrder empty table returns null`() {
        assertNull(serviceRepository.findMaxDisplayOrder(salon.id!!))
    }

    @Test
    fun `findMaxDisplayOrder with data returns max`() {
        val s1 = Service(salonId = salon.id!!, name = "S1", category = "C", displayOrder = 3)
        val s2 = Service(salonId = salon.id!!, name = "S2", category = "C", displayOrder = 7)
        entityManager.persist(s1)
        entityManager.persist(s2)
        entityManager.flush()

        assertEquals(7, serviceRepository.findMaxDisplayOrder(salon.id!!))
    }

    @Test
    fun `countFutureAppointments returns correct count`() {
        val user = User(salonId = salon.id!!, role = UserRole.OWNER, email = "test-${UUID.randomUUID()}@test.com", passwordHash = "hash")
        entityManager.persist(user)
        val employee = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "John")
        entityManager.persist(employee)
        val service = Service(salonId = salon.id!!, name = "Haircut", category = "Hair")
        entityManager.persist(service)

        val client = com.beautyfinder.b2b.domain.client.Client(
            salonId = salon.id!!, firstName = "Jan", lastName = "Kowalski",
        )
        entityManager.persist(client)

        val variant = ServiceVariant(
            serviceId = service.id!!, salonId = salon.id!!, name = "Standard",
            durationMinutes = 30, price = BigDecimal("50.00"),
        )
        entityManager.persist(variant)
        entityManager.flush()

        val futureTime = OffsetDateTime.now().plusDays(1)
        val appointment = Appointment(
            salonId = salon.id!!,
            clientId = client.id!!,
            employeeId = employee.id!!,
            variantId = variant.id!!,
            startAt = futureTime,
            endAt = futureTime.plusMinutes(30),
            status = AppointmentStatus.SCHEDULED,
        )
        entityManager.persist(appointment)
        entityManager.flush()

        val count = variantRepository.countFutureAppointments(
            variant.id!!,
            listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED),
            OffsetDateTime.now(),
        )
        assertEquals(1L, count)
    }
}

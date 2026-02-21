package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.Appointment
import com.beautyfinder.b2b.domain.AppointmentStatus
import com.beautyfinder.b2b.domain.Client
import com.beautyfinder.b2b.domain.Employee
import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.Service
import com.beautyfinder.b2b.domain.ServiceVariant
import com.beautyfinder.b2b.domain.User
import com.beautyfinder.b2b.domain.UserRole
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun `findAllBySalonId returns appointments for given salon`() {
        val salon = Salon(name = "Test Salon", slug = "test-salon")
        entityManager.persist(salon)

        val user = User(
            salonId = salon.id!!,
            role = UserRole.OWNER,
            email = "test@test.com",
            passwordHash = "hash",
        )
        entityManager.persist(user)

        val employee = Employee(
            salonId = salon.id!!,
            userId = user.id!!,
            displayName = "John",
        )
        entityManager.persist(employee)

        val service = Service(
            salonId = salon.id!!,
            name = "Haircut",
            category = "Hair",
        )
        entityManager.persist(service)

        val variant = ServiceVariant(
            serviceId = service.id!!,
            name = "Standard",
            durationMinutes = 30,
            price = BigDecimal("50.00"),
        )
        entityManager.persist(variant)

        val client = Client(
            salonId = salon.id!!,
            name = "Jane Doe",
            phone = "+48123456789",
        )
        entityManager.persist(client)

        val appointment = Appointment(
            salonId = salon.id!!,
            clientId = client.id!!,
            employeeId = employee.id!!,
            variantId = variant.id!!,
            startAt = OffsetDateTime.now(),
            status = AppointmentStatus.SCHEDULED,
            finalPrice = BigDecimal("50.00"),
        )
        entityManager.persist(appointment)
        entityManager.flush()

        val results = appointmentRepository.findAllBySalonId(salon.id!!)

        assertEquals(1, results.size)
        assertEquals(appointment.id, results[0].id)
        assertEquals(salon.id, results[0].salonId)
    }
}

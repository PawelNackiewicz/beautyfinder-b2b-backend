package com.beautyfinder.b2b.infrastructure.client

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.User
import com.beautyfinder.b2b.domain.UserRole
import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.domain.client.ClientStatus
import com.beautyfinder.b2b.infrastructure.ClientRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
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
class ClientRepositoryTest {

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
    private lateinit var clientRepository: ClientRepository

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

    private fun persistClient(
        salonId: UUID = salon.id!!,
        firstName: String = "Jan",
        lastName: String = "Kowalski",
        phone: String? = "+48111222333",
        email: String? = "jan@test.com",
        status: ClientStatus = ClientStatus.ACTIVE,
    ): Client {
        val client = Client(
            salonId = salonId,
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            email = email,
            status = status,
        )
        entityManager.persist(client)
        entityManager.flush()
        return client
    }

    @Test
    fun `searchClients_byPhrase_findsCorrectClients`() {
        persistClient(firstName = "Jan", lastName = "Kowalski", phone = "+48111222333")
        persistClient(firstName = "Anna", lastName = "Nowak", phone = "+48444555666")
        persistClient(firstName = "Janusz", lastName = "Zieliński", phone = "+48777888999")

        val result = clientRepository.searchClients(salon.id!!, "Jan", null, PageRequest.of(0, 20))

        assertEquals(2, result.totalElements) // Jan Kowalski + Janusz Zielinski
    }

    @Test
    fun `searchClients_byStatus_filtersCorrectly`() {
        persistClient(firstName = "Active", lastName = "Client", status = ClientStatus.ACTIVE, phone = "+48111000001")
        persistClient(firstName = "Blocked", lastName = "Client", status = ClientStatus.BLOCKED, phone = "+48111000002")

        val result = clientRepository.searchClients(salon.id!!, null, ClientStatus.BLOCKED, PageRequest.of(0, 20))

        assertEquals(1, result.totalElements)
        assertEquals("Blocked", result.content[0].firstName)
    }

    @Test
    fun `searchClients_crossSalonIsolation`() {
        persistClient(salonId = salon.id!!, firstName = "Salon1", lastName = "Client", phone = "+48111000003")
        persistClient(salonId = salon2.id!!, firstName = "Salon2", lastName = "Client", phone = "+48111000004")

        val result = clientRepository.searchClients(salon.id!!, null, null, PageRequest.of(0, 20))

        assertEquals(1, result.totalElements)
        assertEquals("Salon1", result.content[0].firstName)
    }

    @Test
    fun `updateStatsAfterVisit_incrementsCounterAtomically`() {
        val client = persistClient()

        clientRepository.updateStatsAfterVisit(client.id!!, BigDecimal("100.00"), OffsetDateTime.now(ZoneOffset.UTC))
        entityManager.clear()

        val updated = clientRepository.findById(client.id!!).get()
        assertEquals(1, updated.totalVisits)
        assertEquals(BigDecimal("100.00").setScale(2), updated.totalSpent.setScale(2))
    }

    @Test
    fun `findByPhoneAndSalonId_wrongSalon_returnsNull`() {
        persistClient(salonId = salon.id!!, phone = "+48111222333")

        val result = clientRepository.findByPhoneAndSalonId("+48111222333", salon2.id!!)

        assertNull(result)
    }
}

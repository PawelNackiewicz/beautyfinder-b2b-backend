package com.beautyfinder.b2b.infrastructure.security

import com.beautyfinder.b2b.application.client.ClientService
import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.domain.client.SensitiveDataPayload
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest
@Testcontainers
@Transactional
class EncryptionIntegrationTest {

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
    private lateinit var clientService: ClientService

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var salon: Salon
    private lateinit var client: Client

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Test Salon", slug = "test-${UUID.randomUUID()}")
        entityManager.persist(salon)

        client = Client(salonId = salon.id!!, firstName = "Jan", lastName = "Kowalski", phone = "+48123456789")
        entityManager.persist(client)

        entityManager.flush()
    }

    @Test
    fun `sensitiveData_encryptedAtRest`() {
        val payload = SensitiveDataPayload(
            pesel = "12345678901",
            medicalNotes = "Allergic to latex",
            allergyInfo = "Latex, nickel",
        )

        clientService.upsertSensitiveData(client.id!!, payload, salon.id!!)
        entityManager.flush()
        entityManager.clear()

        // Read raw data from DB
        val rawEncrypted = jdbcTemplate.queryForObject(
            "SELECT encrypted_data FROM client_sensitive_data WHERE client_id = ?",
            String::class.java,
            client.id!!,
        )

        // Verify it does NOT contain plaintext
        assertFalse(rawEncrypted!!.contains("12345678901"), "PESEL should not appear in encrypted data")
        assertFalse(rawEncrypted.contains("Allergic to latex"), "Medical notes should not appear in encrypted data")
    }

    @Test
    fun `sensitiveData_decryptedCorrectly`() {
        val payload = SensitiveDataPayload(
            pesel = "98765432109",
            idNumber = "ABC123456",
            medicalNotes = "No known conditions",
            allergyInfo = "None",
            customFields = mapOf("bloodType" to "A+"),
        )

        clientService.upsertSensitiveData(client.id!!, payload, salon.id!!)
        entityManager.flush()
        entityManager.clear()

        val result = clientService.getSensitiveData(client.id!!, salon.id!!)

        assertEquals("98765432109", result.pesel)
        assertEquals("ABC123456", result.idNumber)
        assertEquals("No known conditions", result.medicalNotes)
        assertEquals("None", result.allergyInfo)
        assertEquals("A+", result.customFields["bloodType"])
    }
}

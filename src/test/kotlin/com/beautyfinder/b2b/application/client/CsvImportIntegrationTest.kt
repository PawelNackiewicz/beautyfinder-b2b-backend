package com.beautyfinder.b2b.application.client

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.infrastructure.ClientRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

@SpringBootTest
@Testcontainers
@Transactional
class CsvImportIntegrationTest {

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
    private lateinit var clientRepository: ClientRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var salon: Salon

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Test Salon", slug = "test-${UUID.randomUUID()}")
        entityManager.persist(salon)
        entityManager.flush()
    }

    @Test
    fun `importValidCsv_allRowsImported`() {
        val csv = """firstName,lastName,phone,email,birthDate,notes
Jan,Kowalski,+48111000001,jan@test.com,1990-01-15,VIP
Anna,Nowak,+48111000002,anna@test.com,,
Piotr,Zieliński,+48111000003,,,
Maria,Wiśniewska,+48111000004,,1985-05-20,Regular
Tomasz,Wójcik,+48111000005,tomasz@test.com,,"""

        val result = clientService.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salon.id!!)

        assertEquals(5, result.total)
        assertEquals(5, result.imported)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `importCsvWithMissingRequiredFields_errorsReported`() {
        val csv = """firstName,lastName,phone,email,birthDate,notes
Jan,Kowalski,+48111000011,jan@test.com,,
Anna,Nowak,+48111000012,,,
,Missing,+48111000013,,,"""

        val result = clientService.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salon.id!!)

        assertEquals(3, result.total)
        assertEquals(2, result.imported)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].errorMessage.contains("firstName"))
    }

    @Test
    fun `importCsvWithDuplicatesInFile_skipsSecondOccurrence`() {
        val csv = """firstName,lastName,phone,email,birthDate,notes
Jan,Kowalski,+48111000021,,,
Anna,Nowak,+48111000021,,,"""

        val result = clientService.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salon.id!!)

        assertEquals(2, result.total)
        assertEquals(1, result.imported)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].errorMessage.contains("Duplicate phone"))
    }

    @Test
    fun `importCsvWithDuplicatesInDb_skipsExisting`() {
        // Pre-persist a client
        val existing = Client(salonId = salon.id!!, firstName = "Existing", lastName = "Client", phone = "+48111000031")
        entityManager.persist(existing)
        entityManager.flush()

        val csv = """firstName,lastName,phone,email,birthDate,notes
Jan,Kowalski,+48111000031,,,
Anna,Nowak,+48111000032,,,"""

        val result = clientService.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salon.id!!)

        assertEquals(2, result.total)
        assertEquals(1, result.imported)
        assertEquals(1, result.skipped) // existing phone skipped
    }

    @Test
    fun `importCsvWithInvalidDate_reportsRowError`() {
        val csv = """firstName,lastName,phone,email,birthDate,notes
Jan,Kowalski,+48111000041,,invalid-date,"""

        val result = clientService.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salon.id!!)

        assertEquals(1, result.total)
        assertEquals(0, result.imported)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].errorMessage.contains("Invalid date"))
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun `importLargeCsv_performance`() {
        val header = "firstName,lastName,phone,email,birthDate,notes"
        val rows = (1..1000).joinToString("\n") { i ->
            "Name$i,Surname$i,+481${String.format("%08d", i)},email$i@test.com,,"
        }
        val csv = "$header\n$rows"

        val result = clientService.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salon.id!!)

        assertEquals(1000, result.total)
        assertEquals(1000, result.imported)
    }
}

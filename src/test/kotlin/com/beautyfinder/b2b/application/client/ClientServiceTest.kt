package com.beautyfinder.b2b.application.client

import com.beautyfinder.b2b.domain.client.BlacklistEntry
import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.domain.client.ClientAlreadyExistsException
import com.beautyfinder.b2b.domain.client.ClientNotFoundException
import com.beautyfinder.b2b.domain.client.ClientSensitiveData
import com.beautyfinder.b2b.domain.client.ClientStatus
import com.beautyfinder.b2b.domain.client.ConsentType
import com.beautyfinder.b2b.domain.client.EncryptionService
import com.beautyfinder.b2b.domain.client.GdprConsent
import com.beautyfinder.b2b.domain.client.GdprConsentAlreadyGrantedException
import com.beautyfinder.b2b.domain.client.InsufficientLoyaltyPointsException
import com.beautyfinder.b2b.domain.client.LoyaltyBalance
import com.beautyfinder.b2b.domain.client.LoyaltyTransactionType
import com.beautyfinder.b2b.domain.client.SensitiveDataNotFoundException
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import com.beautyfinder.b2b.infrastructure.BlacklistRepository
import com.beautyfinder.b2b.infrastructure.ClientRepository
import com.beautyfinder.b2b.infrastructure.ClientSensitiveDataRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.GdprConsentRepository
import com.beautyfinder.b2b.infrastructure.LoyaltyBalanceRepository
import com.beautyfinder.b2b.infrastructure.LoyaltyTransactionRepository
import com.beautyfinder.b2b.infrastructure.ServiceRepository
import com.beautyfinder.b2b.infrastructure.ServiceVariantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ClientServiceTest {

    private val clientRepository: ClientRepository = mockk(relaxed = true)
    private val gdprConsentRepository: GdprConsentRepository = mockk(relaxed = true)
    private val blacklistRepository: BlacklistRepository = mockk(relaxed = true)
    private val loyaltyBalanceRepository: LoyaltyBalanceRepository = mockk(relaxed = true)
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository = mockk(relaxed = true)
    private val clientSensitiveDataRepository: ClientSensitiveDataRepository = mockk(relaxed = true)
    private val appointmentRepository: AppointmentRepository = mockk(relaxed = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxed = true)
    private val serviceVariantRepository: ServiceVariantRepository = mockk(relaxed = true)
    private val serviceRepository: ServiceRepository = mockk(relaxed = true)
    private val encryptionService: EncryptionService = mockk(relaxed = true)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val service = ClientService(
        clientRepository = clientRepository,
        gdprConsentRepository = gdprConsentRepository,
        blacklistRepository = blacklistRepository,
        loyaltyBalanceRepository = loyaltyBalanceRepository,
        loyaltyTransactionRepository = loyaltyTransactionRepository,
        clientSensitiveDataRepository = clientSensitiveDataRepository,
        appointmentRepository = appointmentRepository,
        employeeRepository = employeeRepository,
        serviceVariantRepository = serviceVariantRepository,
        serviceRepository = serviceRepository,
        encryptionService = encryptionService,
        objectMapper = objectMapper,
    )

    private val salonId = UUID.randomUUID()
    private val clientId = UUID.randomUUID()

    private fun buildClient(
        id: UUID = clientId,
        firstName: String = "Jan",
        lastName: String = "Kowalski",
        phone: String? = "+48123456789",
        email: String? = "jan@example.com",
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
        // Use reflection to set id
        val idField = client.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(client, id)
        return client
    }

    private fun buildGdprConsent(
        clientId: UUID = this.clientId,
        consentType: ConsentType = ConsentType.MARKETING_EMAIL,
        granted: Boolean = true,
    ): GdprConsent {
        val consent = GdprConsent(
            clientId = clientId,
            salonId = salonId,
            consentType = consentType,
            granted = granted,
            grantedAt = OffsetDateTime.now(ZoneOffset.UTC),
            consentVersion = "1.0",
        )
        val idField = consent.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(consent, UUID.randomUUID())
        return consent
    }

    private fun buildLoyaltyBalance(
        clientId: UUID = this.clientId,
        points: Int = 100,
    ): LoyaltyBalance {
        val balance = LoyaltyBalance(
            clientId = clientId,
            salonId = salonId,
            points = points,
            totalEarned = points,
        )
        val idField = balance.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(balance, UUID.randomUUID())
        return balance
    }

    // -- Tests --

    @Test
    fun `createClient_success_createsLoyaltyBalance`() {
        val request = CreateClientRequest(
            firstName = "Jan",
            lastName = "Kowalski",
            phone = "+48123456789",
        )
        every { clientRepository.existsByPhoneAndSalonId(any(), any()) } returns false
        every { clientRepository.save(any()) } answers { firstArg<Client>().also {
            val f = it.javaClass.superclass.getDeclaredField("id"); f.isAccessible = true; f.set(it, UUID.randomUUID())
        }}
        every { loyaltyBalanceRepository.save(any()) } answers { firstArg() }
        every { loyaltyBalanceRepository.findByClientIdAndSalonId(any(), any()) } returns null
        every { blacklistRepository.existsByClientIdAndSalonIdAndRemovedAtIsNull(any(), any()) } returns false

        service.createClient(request, salonId)

        verify { loyaltyBalanceRepository.save(any()) }
    }

    @Test
    fun `createClient_duplicatePhone_throwsException`() {
        val request = CreateClientRequest(firstName = "Jan", lastName = "K", phone = "+48123456789")
        every { clientRepository.existsByPhoneAndSalonId("+48123456789", salonId) } returns true

        assertThrows<ClientAlreadyExistsException> {
            service.createClient(request, salonId)
        }
    }

    @Test
    fun `createClient_duplicateEmail_throwsException`() {
        val request = CreateClientRequest(firstName = "Jan", lastName = "K", email = "jan@example.com")
        every { clientRepository.existsByPhoneAndSalonId(any(), any()) } returns false
        every { clientRepository.existsByEmailAndSalonId("jan@example.com", salonId) } returns true

        assertThrows<ClientAlreadyExistsException> {
            service.createClient(request, salonId)
        }
    }

    @Test
    fun `getClientCard_returnsAllSections`() {
        val client = buildClient()
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { gdprConsentRepository.findAllByClientIdAndSalonId(clientId, salonId) } returns listOf(buildGdprConsent())
        every { blacklistRepository.findByClientIdAndSalonIdAndRemovedAtIsNull(clientId, salonId) } returns null
        every { blacklistRepository.existsByClientIdAndSalonIdAndRemovedAtIsNull(clientId, salonId) } returns false
        every { loyaltyBalanceRepository.findByClientIdAndSalonId(clientId, salonId) } returns buildLoyaltyBalance()
        every { appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, any(), any()) } returns emptyList()

        val card = service.getClientCard(clientId, salonId)

        assertEquals("Jan", card.client.firstName)
        assertEquals(1, card.consents.size)
        assertNotNull(card.loyaltyBalance)
        assertNotNull(card.stats)
    }

    @Test
    fun `getSensitiveData_decryptsCorrectly`() {
        val client = buildClient()
        val payload = """{"pesel":"12345678901","medicalNotes":"test notes"}"""
        val sensitiveData = ClientSensitiveData(clientId = clientId, encryptedData = "encrypted", dataHash = "hash")

        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { clientSensitiveDataRepository.findByClientId(clientId) } returns sensitiveData
        every { encryptionService.decrypt("encrypted") } returns payload

        val result = service.getSensitiveData(clientId, salonId)

        assertEquals("12345678901", result.pesel)
        assertEquals("test notes", result.medicalNotes)
    }

    @Test
    fun `getSensitiveData_notFound_throwsException`() {
        val client = buildClient()
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { clientSensitiveDataRepository.findByClientId(clientId) } returns null

        assertThrows<SensitiveDataNotFoundException> {
            service.getSensitiveData(clientId, salonId)
        }
    }

    @Test
    fun `upsertSensitiveData_newRecord_savesEncrypted`() {
        val client = buildClient()
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { clientSensitiveDataRepository.findByClientId(clientId) } returns null
        every { encryptionService.encrypt(any()) } returns "encrypted_value"
        every { encryptionService.hash(any()) } returns "hash_value"
        every { clientSensitiveDataRepository.save(any()) } answers { firstArg() }

        service.upsertSensitiveData(clientId, com.beautyfinder.b2b.domain.client.SensitiveDataPayload(pesel = "12345678901"), salonId)

        verify { clientSensitiveDataRepository.save(match { it.encryptedData == "encrypted_value" }) }
    }

    @Test
    fun `upsertSensitiveData_existingRecord_updatesEncrypted`() {
        val client = buildClient()
        val existing = ClientSensitiveData(clientId = clientId, encryptedData = "old", dataHash = "old_hash")
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { clientSensitiveDataRepository.findByClientId(clientId) } returns existing
        every { encryptionService.encrypt(any()) } returns "new_encrypted"
        every { encryptionService.hash(any()) } returns "new_hash"
        every { clientSensitiveDataRepository.save(any()) } answers { firstArg() }

        service.upsertSensitiveData(clientId, com.beautyfinder.b2b.domain.client.SensitiveDataPayload(pesel = "99999999999"), salonId)

        verify { clientSensitiveDataRepository.save(match { it.encryptedData == "new_encrypted" && it.dataHash == "new_hash" }) }
    }

    @Test
    fun `recordGdprConsent_newConsent_saves`() {
        val client = buildClient()
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { gdprConsentRepository.findByClientIdAndConsentTypeAndGrantedTrue(clientId, ConsentType.MARKETING_EMAIL) } returns null
        every { gdprConsentRepository.save(any()) } answers {
            firstArg<GdprConsent>().also {
                val f = it.javaClass.superclass.getDeclaredField("id"); f.isAccessible = true; f.set(it, UUID.randomUUID())
            }
        }

        val result = service.recordGdprConsent(
            clientId,
            GdprConsentRequest(ConsentType.MARKETING_EMAIL, true, "1.0"),
            salonId,
        )

        assertTrue(result.granted)
        verify { gdprConsentRepository.save(any()) }
    }

    @Test
    fun `recordGdprConsent_alreadyGranted_throwsException`() {
        val client = buildClient()
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { gdprConsentRepository.findByClientIdAndConsentTypeAndGrantedTrue(clientId, ConsentType.MARKETING_EMAIL) } returns buildGdprConsent()

        assertThrows<GdprConsentAlreadyGrantedException> {
            service.recordGdprConsent(
                clientId,
                GdprConsentRequest(ConsentType.MARKETING_EMAIL, true, "1.0"),
                salonId,
            )
        }
    }

    @Test
    fun `revokeGdprConsent_appendsNewRecord`() {
        val client = buildClient()
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { gdprConsentRepository.save(any()) } answers { firstArg() }

        service.revokeGdprConsent(clientId, ConsentType.MARKETING_EMAIL, salonId)

        verify { gdprConsentRepository.save(match { !it.granted }) }
    }

    @Test
    fun `addToBlacklist_setsClientBlocked`() {
        val client = buildClient()
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { blacklistRepository.findByClientIdAndSalonIdAndRemovedAtIsNull(clientId, salonId) } returns null
        every { blacklistRepository.save(any()) } answers { firstArg() }
        every { clientRepository.save(any()) } answers { firstArg() }

        service.addToBlacklist(clientId, "spam", UUID.randomUUID(), salonId)

        verify { blacklistRepository.save(any()) }
        assertEquals(ClientStatus.BLOCKED, client.status)
    }

    @Test
    fun `removeFromBlacklist_setsClientActive`() {
        val client = buildClient(status = ClientStatus.BLOCKED)
        val entry = BlacklistEntry(clientId = clientId, salonId = salonId, reason = "spam", createdBy = UUID.randomUUID())
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns client
        every { blacklistRepository.findByClientIdAndSalonIdAndRemovedAtIsNull(clientId, salonId) } returns entry
        every { blacklistRepository.save(any()) } answers { firstArg() }
        every { clientRepository.save(any()) } answers { firstArg() }

        service.removeFromBlacklist(clientId, UUID.randomUUID(), salonId)

        assertNotNull(entry.removedAt)
        assertEquals(ClientStatus.ACTIVE, client.status)
    }

    @Test
    fun `addLoyaltyPoints_earn_updatesBalance`() {
        val balance = buildLoyaltyBalance(points = 50)
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns buildClient()
        every { loyaltyBalanceRepository.findByClientIdAndSalonId(clientId, salonId) } returns balance
        every { loyaltyBalanceRepository.save(any()) } answers { firstArg() }
        every { loyaltyTransactionRepository.save(any()) } answers { firstArg() }

        val result = service.addLoyaltyPoints(clientId, 30, LoyaltyTransactionType.VISIT_REWARD, null, null, salonId)

        assertEquals(80, result.points)
    }

    @Test
    fun `addLoyaltyPoints_redeem_insufficient_throwsException`() {
        val balance = buildLoyaltyBalance(points = 10)
        every { clientRepository.findByIdAndSalonId(clientId, salonId) } returns buildClient()
        every { loyaltyBalanceRepository.findByClientIdAndSalonId(clientId, salonId) } returns balance

        assertThrows<InsufficientLoyaltyPointsException> {
            service.addLoyaltyPoints(clientId, -50, LoyaltyTransactionType.REDEMPTION, null, null, salonId)
        }
    }

    @Test
    fun `importClientsFromCsv_validFile_importsAll`() {
        val csv = """firstName,lastName,phone,email,birthDate,notes
Jan,Kowalski,+48111222333,jan@test.com,1990-01-15,VIP
Anna,Nowak,+48444555666,anna@test.com,,Regular"""

        every { clientRepository.existsByPhoneAndSalonId(any(), any()) } returns false
        every { clientRepository.saveAll(any<List<Client>>()) } answers { firstArg() }

        val result = service.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salonId)

        assertEquals(2, result.total)
        assertEquals(2, result.imported)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `importClientsFromCsv_partialErrors_importsValidRows`() {
        val csv = """firstName,lastName,phone,email,birthDate,notes
Jan,Kowalski,+48111222333,jan@test.com,,
,,,,,
Anna,Nowak,+48444555666,anna@test.com,,"""

        every { clientRepository.existsByPhoneAndSalonId(any(), any()) } returns false
        every { clientRepository.saveAll(any<List<Client>>()) } answers { firstArg() }

        val result = service.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salonId)

        assertEquals(3, result.total)
        assertEquals(2, result.imported)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `importClientsFromCsv_duplicatePhone_marksAsDuplicate`() {
        val csv = """firstName,lastName,phone,email,birthDate,notes
Jan,Kowalski,+48111222333,jan@test.com,,
Anna,Nowak,+48111222333,anna@test.com,,"""

        every { clientRepository.existsByPhoneAndSalonId(any(), any()) } returns false
        every { clientRepository.saveAll(any<List<Client>>()) } answers { firstArg() }

        val result = service.importClientsFromCsv(ByteArrayInputStream(csv.toByteArray()), salonId)

        assertEquals(2, result.total)
        assertEquals(1, result.imported)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].errorMessage.contains("Duplicate phone"))
    }
}

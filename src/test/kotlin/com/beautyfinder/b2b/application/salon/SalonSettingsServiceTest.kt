package com.beautyfinder.b2b.application.salon

import com.beautyfinder.b2b.domain.BaseEntity
import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.client.EncryptionService
import com.beautyfinder.b2b.domain.salon.InvalidLoyaltyConfigException
import com.beautyfinder.b2b.domain.salon.InvalidOpeningHoursException
import com.beautyfinder.b2b.domain.salon.InvalidTimezoneException
import com.beautyfinder.b2b.domain.salon.LoyaltyConfig
import com.beautyfinder.b2b.domain.salon.SalonIntegrationSettings
import com.beautyfinder.b2b.domain.salon.SalonNotificationSettings
import com.beautyfinder.b2b.domain.salon.SalonOpeningHours
import com.beautyfinder.b2b.domain.salon.SalonSlugAlreadyExistsException
import com.beautyfinder.b2b.domain.salon.SalonStatus
import com.beautyfinder.b2b.infrastructure.salon.SalonCacheService
import com.beautyfinder.b2b.infrastructure.salon.SalonIntegrationSettingsRepository
import com.beautyfinder.b2b.infrastructure.salon.SalonNotificationSettingsRepository
import com.beautyfinder.b2b.infrastructure.salon.SalonOpeningHoursRepository
import com.beautyfinder.b2b.infrastructure.salon.SalonRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class SalonSettingsServiceTest {

    private val salonRepository = mockk<SalonRepository>(relaxed = true)
    private val openingHoursRepository = mockk<SalonOpeningHoursRepository>(relaxed = true)
    private val notificationSettingsRepository = mockk<SalonNotificationSettingsRepository>()
    private val integrationSettingsRepository = mockk<SalonIntegrationSettingsRepository>()
    private val cacheService = mockk<SalonCacheService>(relaxed = true)
    private val storageService = mockk<StorageService>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val encryptionService = mockk<EncryptionService>()

    private val service = SalonSettingsService(
        salonRepository = salonRepository,
        openingHoursRepository = openingHoursRepository,
        notificationSettingsRepository = notificationSettingsRepository,
        integrationSettingsRepository = integrationSettingsRepository,
        cacheService = cacheService,
        storageService = storageService,
        eventPublisher = eventPublisher,
        encryptionService = encryptionService,
    )

    private val salonId = UUID.randomUUID()

    // --- getSalonSettings ---

    @Test
    fun `getSalonSettings_cacheHit_doesNotQueryDb`() {
        // given
        val cachedDto = buildSettingsDto()
        every { cacheService.get(salonId) } returns cachedDto

        // when
        val result = service.getSalonSettings(salonId)

        // then
        assertEquals(cachedDto, result)
        verify(exactly = 0) { salonRepository.findById(any()) }
    }

    @Test
    fun `getSalonSettings_cacheMiss_queriesDbAndCaches`() {
        // given
        val salon = buildSalon()
        every { cacheService.get(salonId) } returns null
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        every { openingHoursRepository.findAllBySalonIdOrderByDayOfWeek(salonId) } returns emptyList()
        every { notificationSettingsRepository.findBySalonId(salonId) } returns null
        every { integrationSettingsRepository.findBySalonId(salonId) } returns null

        // when
        val result = service.getSalonSettings(salonId)

        // then
        assertNotNull(result)
        assertEquals(salonId, result.id)
        verify { cacheService.put(salonId, any()) }
    }

    // --- updateGeneralSettings ---

    @Test
    fun `updateGeneralSettings_evictsCache`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        every { salonRepository.existsBySlugAndIdNot(any(), any()) } returns false
        stubGetSalonSettingsAfterUpdate(salon)
        val request = UpdateGeneralSettingsRequest(name = "New Name")

        // when
        service.updateGeneralSettings(salonId, request)

        // then
        verify { cacheService.evict(salonId) }
    }

    @Test
    fun `updateGeneralSettings_slugConflict_throwsException`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        every { salonRepository.existsBySlugAndIdNot("taken-slug", salonId) } returns true
        val request = UpdateGeneralSettingsRequest(slug = "taken-slug")

        // when/then
        assertThrows<SalonSlugAlreadyExistsException> {
            service.updateGeneralSettings(salonId, request)
        }
    }

    @Test
    fun `updateGeneralSettings_invalidTimezone_throwsException`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        val request = UpdateGeneralSettingsRequest(timezone = "Mars/Olympus")

        // when/then
        assertThrows<InvalidTimezoneException> {
            service.updateGeneralSettings(salonId, request)
        }
    }

    @Test
    fun `updateGeneralSettings_publishesEvent`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        every { salonRepository.existsBySlugAndIdNot(any(), any()) } returns false
        stubGetSalonSettingsAfterUpdate(salon)
        val request = UpdateGeneralSettingsRequest(name = "Updated Salon", phone = "+48123456789")

        // when
        service.updateGeneralSettings(salonId, request)

        // then
        val eventSlot = slot<SalonSettingsUpdatedEvent>()
        verify { eventPublisher.publishEvent(capture(eventSlot)) }
        assertEquals(salonId, eventSlot.captured.salonId)
        assert(eventSlot.captured.changedFields.contains("name"))
        assert(eventSlot.captured.changedFields.contains("phone"))
    }

    // --- updateOpeningHours ---

    @Test
    fun `updateOpeningHours_replacesAll`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        every { openingHoursRepository.deleteAllBySalonId(salonId) } just runs
        every { openingHoursRepository.saveAll(any<List<SalonOpeningHours>>()) } answers { firstArg() }

        val request = listOf(
            UpdateOpeningHoursRequest(
                dayOfWeek = DayOfWeek.MONDAY,
                isOpen = true,
                openTime = LocalTime.of(9, 0),
                closeTime = LocalTime.of(17, 0),
            ),
        )

        // when
        service.updateOpeningHours(salonId, request)

        // then
        verify { openingHoursRepository.deleteAllBySalonId(salonId) }
        verify { openingHoursRepository.saveAll(any<List<SalonOpeningHours>>()) }
    }

    @Test
    fun `updateOpeningHours_invalidHours_throwsException`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)

        val request = listOf(
            UpdateOpeningHoursRequest(
                dayOfWeek = DayOfWeek.MONDAY,
                isOpen = true,
                openTime = LocalTime.of(17, 0),
                closeTime = LocalTime.of(9, 0), // closeTime before openTime
            ),
        )

        // when/then
        assertThrows<InvalidOpeningHoursException> {
            service.updateOpeningHours(salonId, request)
        }
    }

    // --- updateLoyaltySettings ---

    @Test
    fun `updateLoyaltySettings_enabledNoRedemptionRate_throwsException`() {
        // given
        val salon = buildSalon(loyaltyEnabled = false)
        every { salonRepository.findById(salonId) } returns Optional.of(salon)

        val request = UpdateLoyaltySettingsRequest(
            enabled = true,
            pointsPerVisit = 10,
            pointsPerCurrencyUnit = null,
            redemptionRate = null, // missing redemption rate
            expireDays = 365,
        )

        // when/then
        assertThrows<InvalidLoyaltyConfigException> {
            service.updateLoyaltySettings(salonId, request)
        }
    }

    @Test
    fun `updateLoyaltySettings_justEnabled_publishesLoyaltyEvent`() {
        // given
        val salon = buildSalon(loyaltyEnabled = false) // was disabled
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        stubGetSalonSettingsAfterUpdate(salon)

        val request = UpdateLoyaltySettingsRequest(
            enabled = true,
            pointsPerVisit = 10,
            pointsPerCurrencyUnit = null,
            redemptionRate = BigDecimal("0.50"),
            expireDays = 365,
        )

        // when
        service.updateLoyaltySettings(salonId, request)

        // then
        val eventSlot = slot<LoyaltyProgramEnabledEvent>()
        verify { eventPublisher.publishEvent(capture(eventSlot)) }
        assertEquals(salonId, eventSlot.captured.salonId)
    }

    @Test
    fun `updateLoyaltySettings_alreadyEnabled_noLoyaltyEvent`() {
        // given
        val salon = buildSalon(loyaltyEnabled = true) // already enabled
        salon.pointsPerVisit = 5
        salon.pointsRedemptionRate = BigDecimal("0.25")
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        stubGetSalonSettingsAfterUpdate(salon)

        val request = UpdateLoyaltySettingsRequest(
            enabled = true,
            pointsPerVisit = 20,
            pointsPerCurrencyUnit = null,
            redemptionRate = BigDecimal("0.75"),
            expireDays = 180,
        )

        // when
        service.updateLoyaltySettings(salonId, request)

        // then
        verify(exactly = 0) { eventPublisher.publishEvent(any<LoyaltyProgramEnabledEvent>()) }
    }

    // --- updateIntegrationSettings ---

    @Test
    fun `updateIntegrationSettings_webhookSecretEncrypted`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        every { integrationSettingsRepository.findBySalonId(salonId) } returns SalonIntegrationSettings(salonId = salonId)
        every { encryptionService.encrypt("my-secret") } returns "encrypted-my-secret"
        every { integrationSettingsRepository.save(any()) } answers { firstArg() }

        val request = UpdateIntegrationSettingsRequest(webhookSecret = "my-secret")

        // when
        service.updateIntegrationSettings(salonId, request)

        // then
        verify { encryptionService.encrypt("my-secret") }
        val settingsSlot = slot<SalonIntegrationSettings>()
        verify { integrationSettingsRepository.save(capture(settingsSlot)) }
        assertEquals("encrypted-my-secret", settingsSlot.captured.webhookSecret)
    }

    // --- uploadLogo ---

    @Test
    fun `uploadLogo_validImage_storesAndUpdates`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        val file = mockk<MultipartFile>()
        every { file.contentType } returns "image/png"
        every { file.size } returns 500_000L
        every { file.inputStream } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))
        every { storageService.store(any(), any(), eq("image/png")) } returns "https://cdn.example.com/logo.png"

        // when
        val url = service.uploadLogo(salonId, file)

        // then
        assertEquals("https://cdn.example.com/logo.png", url)
        verify { storageService.store(any(), any(), "image/png") }
        assertEquals("https://cdn.example.com/logo.png", salon.logoUrl)
        verify { salonRepository.save(salon) }
    }

    @Test
    fun `uploadLogo_invalidContentType_throwsException`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        val file = mockk<MultipartFile>()
        every { file.contentType } returns "text/plain"

        // when/then
        assertThrows<IllegalArgumentException> {
            service.uploadLogo(salonId, file)
        }
    }

    @Test
    fun `uploadLogo_tooLargeFile_throwsException`() {
        // given
        val salon = buildSalon()
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        val file = mockk<MultipartFile>()
        every { file.contentType } returns "image/jpeg"
        every { file.size } returns 3 * 1024 * 1024L // 3MB exceeds 2MB limit

        // when/then
        assertThrows<IllegalArgumentException> {
            service.uploadLogo(salonId, file)
        }
    }

    // --- getLoyaltyConfigForAppointment ---

    @Test
    fun `getLoyaltyConfigForAppointment_disabled_returnsNull`() {
        // given
        val salon = buildSalon(loyaltyEnabled = false)
        every { salonRepository.findById(salonId) } returns Optional.of(salon)

        // when
        val result = service.getLoyaltyConfigForAppointment(salonId)

        // then
        assertNull(result)
    }

    @Test
    fun `getLoyaltyConfigForAppointment_enabled_returnsConfig`() {
        // given
        val salon = buildSalon(loyaltyEnabled = true)
        salon.pointsPerVisit = 10
        salon.pointsPerCurrencyUnit = 5
        salon.pointsRedemptionRate = BigDecimal("0.50")
        salon.loyaltyPointsExpireDays = 365
        every { salonRepository.findById(salonId) } returns Optional.of(salon)

        // when
        val result = service.getLoyaltyConfigForAppointment(salonId)

        // then
        assertNotNull(result)
        assertEquals(true, result!!.enabled)
        assertEquals(10, result.pointsPerVisit)
        assertEquals(5, result.pointsPerCurrencyUnit)
        assertEquals(BigDecimal("0.50"), result.redemptionRate)
        assertEquals(365, result.expireDays)
    }

    // --- Helpers ---

    private fun buildSalon(loyaltyEnabled: Boolean = false): Salon {
        val salon = Salon(
            name = "Test Salon",
            slug = "test-salon",
            status = SalonStatus.ACTIVE,
            timezone = "Europe/Warsaw",
            currency = "PLN",
            cancellationWindowHours = 24,
            bookingWindowDays = 60,
            slotIntervalMinutes = 15,
            defaultAppointmentBufferMinutes = 0,
            maxAdvanceBookingDays = 90,
            loyaltyEnabled = loyaltyEnabled,
            phone = "+48123456789",
            email = "salon@test.com",
        )
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(salon, salonId)
        return salon
    }

    private fun buildSettingsDto(): SalonSettingsDto = SalonSettingsDto(
        id = salonId,
        name = "Test Salon",
        slug = "test-salon",
        status = "ACTIVE",
        logoUrl = null,
        coverImageUrl = null,
        description = null,
        websiteUrl = null,
        phone = "+48123456789",
        email = "salon@test.com",
        address = AddressDto(
            street = null,
            city = null,
            postalCode = null,
            country = "PL",
            latitude = null,
            longitude = null,
            formatted = null,
        ),
        timezone = "Europe/Warsaw",
        currency = "PLN",
        cancellationWindowHours = 24,
        bookingWindowDays = 60,
        slotIntervalMinutes = 15,
        defaultAppointmentBufferMinutes = 0,
        invoicing = InvoicingDto(
            invoicingName = null,
            taxId = null,
            street = null,
            city = null,
            postalCode = null,
            bankAccountNumber = null,
            footerNotes = null,
        ),
        loyalty = LoyaltySettingsDto(
            enabled = false,
            pointsPerVisit = null,
            pointsPerCurrencyUnit = null,
            redemptionRate = null,
            expireDays = null,
        ),
        booking = BookingPolicyDto(
            requireClientPhone = true,
            allowOnlineBooking = true,
            autoConfirmAppointments = false,
            onlineBookingMessage = null,
            maxAdvanceBookingDays = 90,
        ),
        openingHours = emptyList(),
        notifications = null,
        integrations = null,
        createdAt = null,
        updatedAt = null,
    )

    /**
     * Stubs the dependencies needed for getSalonSettings() which is called at the end of
     * update methods to return the refreshed DTO.
     */
    private fun stubGetSalonSettingsAfterUpdate(salon: Salon) {
        // After evict, cache returns null so service queries DB again
        every { cacheService.get(salonId) } returns null
        every { salonRepository.findById(salonId) } returns Optional.of(salon)
        every { openingHoursRepository.findAllBySalonIdOrderByDayOfWeek(salonId) } returns emptyList()
        every { notificationSettingsRepository.findBySalonId(salonId) } returns null
        every { integrationSettingsRepository.findBySalonId(salonId) } returns null
    }
}

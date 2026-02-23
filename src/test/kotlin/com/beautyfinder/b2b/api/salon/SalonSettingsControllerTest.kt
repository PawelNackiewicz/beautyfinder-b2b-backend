package com.beautyfinder.b2b.api.salon

import com.beautyfinder.b2b.application.salon.AddressDto
import com.beautyfinder.b2b.application.salon.BookingPolicyDto
import com.beautyfinder.b2b.application.salon.InvoicingDto
import com.beautyfinder.b2b.application.salon.LoyaltySettingsDto
import com.beautyfinder.b2b.application.salon.SalonIntegrationSettingsDto
import com.beautyfinder.b2b.application.salon.SalonNotificationSettingsDto
import com.beautyfinder.b2b.application.salon.SalonOpeningHoursDto
import com.beautyfinder.b2b.application.salon.SalonPublicProfileDto
import com.beautyfinder.b2b.application.salon.SalonSettingsDto
import com.beautyfinder.b2b.application.salon.SalonSettingsService
import com.beautyfinder.b2b.config.JwtAuthenticationFilter
import com.beautyfinder.b2b.config.JwtService
import com.beautyfinder.b2b.config.SecurityConfig
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.salon.InvalidLoyaltyConfigException
import com.beautyfinder.b2b.domain.salon.InvalidOpeningHoursException
import com.beautyfinder.b2b.domain.salon.InvalidTimezoneException
import com.beautyfinder.b2b.domain.salon.SalonNotFoundException
import com.beautyfinder.b2b.domain.salon.SalonSlugAlreadyExistsException
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@WebMvcTest(SalonSettingsController::class)
@Import(SalonSettingsControllerTest.MockConfig::class, SecurityConfig::class)
class SalonSettingsControllerTest {

    @TestConfiguration
    class MockConfig {
        @Bean
        fun salonSettingsService(): SalonSettingsService = io.mockk.mockk()

        @Bean
        fun jwtService(): JwtService = io.mockk.mockk()

        @Bean
        fun jwtAuthenticationFilter(jwtService: JwtService): JwtAuthenticationFilter =
            JwtAuthenticationFilter(jwtService)
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var salonSettingsService: SalonSettingsService

    companion object {
        val salonId: UUID = UUID.fromString("a0000000-0000-0000-0000-000000000001")
    }

    @BeforeEach
    fun setUp() {
        mockkObject(TenantContext)
        every { TenantContext.getSalonId() } returns salonId
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TenantContext)
    }

    // ==================== Helper functions ====================

    private fun buildSettingsDto(): SalonSettingsDto = SalonSettingsDto(
        id = salonId,
        name = "Beauty Studio",
        slug = "beauty-studio",
        status = "ACTIVE",
        logoUrl = null,
        coverImageUrl = null,
        description = "A beautiful salon",
        websiteUrl = "https://beauty-studio.pl",
        phone = "+48123456789",
        email = "contact@beauty-studio.pl",
        address = AddressDto(
            street = "Marszalkowska 1",
            city = "Warsaw",
            postalCode = "00-001",
            country = "PL",
            latitude = null,
            longitude = null,
            formatted = "Marszalkowska 1, 00-001 Warsaw",
        ),
        timezone = "Europe/Warsaw",
        currency = "PLN",
        cancellationWindowHours = 24,
        bookingWindowDays = 60,
        slotIntervalMinutes = 15,
        defaultAppointmentBufferMinutes = 0,
        invoicing = InvoicingDto(
            invoicingName = "Beauty Studio Sp. z o.o.",
            taxId = "1234567890",
            street = "Marszalkowska 1",
            city = "Warsaw",
            postalCode = "00-001",
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
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
    )

    private fun buildPublicProfileDto(): SalonPublicProfileDto = SalonPublicProfileDto(
        id = salonId,
        name = "Beauty Studio",
        slug = "beauty-studio",
        logoUrl = null,
        coverImageUrl = null,
        description = "A beautiful salon",
        phone = "+48123456789",
        email = "contact@beauty-studio.pl",
        address = AddressDto(
            street = "Marszalkowska 1",
            city = "Warsaw",
            postalCode = "00-001",
            country = "PL",
            latitude = null,
            longitude = null,
            formatted = "Marszalkowska 1, 00-001 Warsaw",
        ),
        openingHours = emptyList(),
        timezone = "Europe/Warsaw",
    )

    private fun buildOpeningHoursDto(): List<SalonOpeningHoursDto> = listOf(
        SalonOpeningHoursDto(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null),
        SalonOpeningHoursDto(DayOfWeek.TUESDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null),
        SalonOpeningHoursDto(DayOfWeek.WEDNESDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null),
        SalonOpeningHoursDto(DayOfWeek.THURSDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null),
        SalonOpeningHoursDto(DayOfWeek.FRIDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null),
        SalonOpeningHoursDto(DayOfWeek.SATURDAY, true, LocalTime.of(9, 0), LocalTime.of(14, 0), null, null),
        SalonOpeningHoursDto(DayOfWeek.SUNDAY, false, null, null, null, null),
    )

    private fun buildNotificationSettingsDto(): SalonNotificationSettingsDto = SalonNotificationSettingsDto(
        reminderEnabled = true,
        reminderHoursBefore = 24,
        confirmationEnabled = true,
        cancellationNotificationEnabled = true,
        smsEnabled = false,
        emailEnabled = true,
        notificationEmail = "notify@beauty-studio.pl",
        notificationPhone = null,
    )

    private fun buildIntegrationSettingsDto(): SalonIntegrationSettingsDto = SalonIntegrationSettingsDto(
        googleCalendarEnabled = false,
        facebookPixelId = null,
        googleAnalyticsId = null,
        webhookUrl = null,
        webhookConfigured = false,
        marketplaceEnabled = false,
        marketplaceProfileVisible = false,
    )

    // ==================== Tests ====================

    // a) getSettings_ownerRole_returns200
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getSettings ownerRole returns200`() {
        every { salonSettingsService.getSalonSettings(salonId) } returns buildSettingsDto()

        mockMvc.get("/api/salon/settings").andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Beauty Studio") }
            jsonPath("$.slug") { value("beauty-studio") }
        }
    }

    // b) getSettings_employeeRole_returns403
    @Test
    @WithMockUser(roles = ["EMPLOYEE"])
    fun `getSettings employeeRole returns403`() {
        mockMvc.get("/api/salon/settings").andExpect {
            status { isForbidden() }
        }
    }

    // c) updateGeneral_validRequest_returns200
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateGeneral validRequest returns200`() {
        every { salonSettingsService.updateGeneralSettings(salonId, any()) } returns buildSettingsDto()

        val request = mapOf(
            "name" to "Updated Studio",
            "slug" to "updated-studio",
            "timezone" to "Europe/Warsaw",
        )

        mockMvc.put("/api/salon/settings/general") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Beauty Studio") }
        }
    }

    // d) updateGeneral_invalidSlugFormat_returns400
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateGeneral invalidSlugFormat returns400`() {
        val request = mapOf(
            "slug" to "My Salon!",
        )

        mockMvc.put("/api/salon/settings/general") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // e) updateGeneral_slugConflict_returns409
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateGeneral slugConflict returns409`() {
        every {
            salonSettingsService.updateGeneralSettings(salonId, any())
        } throws SalonSlugAlreadyExistsException("beauty-studio")

        val request = mapOf(
            "slug" to "beauty-studio",
        )

        mockMvc.put("/api/salon/settings/general") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
        }
    }

    // f) updateGeneral_invalidTimezone_returns400
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateGeneral invalidTimezone returns400`() {
        every {
            salonSettingsService.updateGeneralSettings(salonId, any())
        } throws InvalidTimezoneException("Invalid/Zone")

        val request = mapOf(
            "timezone" to "Invalid/Zone",
        )

        mockMvc.put("/api/salon/settings/general") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // g) updateGeneral_managerRole_returns403
    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `updateGeneral managerRole returns403`() {
        val request = mapOf(
            "name" to "Should Not Work",
        )

        mockMvc.put("/api/salon/settings/general") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }

    // h) updateOpeningHours_validWeek_returns200
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateOpeningHours validWeek returns200`() {
        every { salonSettingsService.updateOpeningHours(salonId, any()) } returns buildOpeningHoursDto()

        val request = listOf(
            mapOf("dayOfWeek" to "MONDAY", "isOpen" to true, "openTime" to "09:00", "closeTime" to "18:00"),
            mapOf("dayOfWeek" to "TUESDAY", "isOpen" to true, "openTime" to "09:00", "closeTime" to "18:00"),
            mapOf("dayOfWeek" to "WEDNESDAY", "isOpen" to true, "openTime" to "09:00", "closeTime" to "18:00"),
            mapOf("dayOfWeek" to "THURSDAY", "isOpen" to true, "openTime" to "09:00", "closeTime" to "18:00"),
            mapOf("dayOfWeek" to "FRIDAY", "isOpen" to true, "openTime" to "09:00", "closeTime" to "18:00"),
            mapOf("dayOfWeek" to "SATURDAY", "isOpen" to true, "openTime" to "09:00", "closeTime" to "14:00"),
            mapOf("dayOfWeek" to "SUNDAY", "isOpen" to false),
        )

        mockMvc.put("/api/salon/settings/opening-hours") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].dayOfWeek") { value("MONDAY") }
            jsonPath("$[6].isOpen") { value(false) }
        }
    }

    // i) updateOpeningHours_missingOpenTime_returns400
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateOpeningHours missingOpenTime returns400`() {
        every {
            salonSettingsService.updateOpeningHours(salonId, any())
        } throws InvalidOpeningHoursException(DayOfWeek.MONDAY, "openTime is required when salon is open")

        val request = listOf(
            mapOf("dayOfWeek" to "MONDAY", "isOpen" to true, "openTime" to null, "closeTime" to "18:00"),
        )

        mockMvc.put("/api/salon/settings/opening-hours") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // j) updateOpeningHours_closeBeforeOpen_returns400
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateOpeningHours closeBeforeOpen returns400`() {
        every {
            salonSettingsService.updateOpeningHours(salonId, any())
        } throws InvalidOpeningHoursException(DayOfWeek.MONDAY, "closeTime must be after openTime")

        val request = listOf(
            mapOf("dayOfWeek" to "MONDAY", "isOpen" to true, "openTime" to "18:00", "closeTime" to "09:00"),
        )

        mockMvc.put("/api/salon/settings/opening-hours") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // k) updateLoyalty_enabled_noRedemptionRate_returns400
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateLoyalty enabled noRedemptionRate returns400`() {
        every {
            salonSettingsService.updateLoyaltySettings(salonId, any())
        } throws InvalidLoyaltyConfigException(listOf("Redemption rate is required when loyalty is enabled"))

        val request = mapOf(
            "enabled" to true,
            "pointsPerVisit" to 10,
        )

        mockMvc.put("/api/salon/settings/loyalty") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // l) updateLoyalty_disabled_noValidation_returns200
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateLoyalty disabled noValidation returns200`() {
        every { salonSettingsService.updateLoyaltySettings(salonId, any()) } returns buildSettingsDto()

        val request = mapOf(
            "enabled" to false,
        )

        mockMvc.put("/api/salon/settings/loyalty") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }
    }

    // m) uploadLogo_validJpeg_returns200WithUrl
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `uploadLogo validJpeg returns200WithUrl`() {
        every { salonSettingsService.uploadLogo(salonId, any()) } returns "https://cdn.example.com/logo.jpg"

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/salon/settings/logo")
                .file(MockMultipartFile("file", "logo.jpg", "image/jpeg", "image-data".toByteArray()))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.logoUrl").value("https://cdn.example.com/logo.jpg"))
    }

    // n) uploadLogo_invalidType_returns400
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `uploadLogo invalidType returns400`() {
        every {
            salonSettingsService.uploadLogo(salonId, any())
        } throws IllegalArgumentException("Invalid file type: application/pdf. Allowed: [image/jpeg, image/png, image/webp]")

        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/salon/settings/logo")
                .file(MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf-data".toByteArray()))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    // o) getPublicProfile_noAuth_returns200
    @Test
    fun `getPublicProfile noAuth returns200`() {
        every { salonSettingsService.getSalonPublicProfile("beauty-studio") } returns buildPublicProfileDto()

        mockMvc.get("/api/salon/public/beauty-studio").andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Beauty Studio") }
            jsonPath("$.slug") { value("beauty-studio") }
        }
    }

    // p) getPublicProfile_unknownSlug_returns404
    @Test
    fun `getPublicProfile unknownSlug returns404`() {
        every {
            salonSettingsService.getSalonPublicProfile("unknown-slug")
        } throws SalonNotFoundException(UUID(0, 0))

        mockMvc.get("/api/salon/public/unknown-slug").andExpect {
            status { isNotFound() }
        }
    }

    // q) updateInvoicing_validNip_returns200
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateInvoicing validNip returns200`() {
        every { salonSettingsService.updateInvoicingSettings(salonId, any()) } returns buildSettingsDto()

        val request = mapOf(
            "invoicingName" to "Beauty Studio Sp. z o.o.",
            "taxId" to "1234567890",
            "invoicingStreet" to "Marszalkowska 1",
            "invoicingCity" to "Warsaw",
            "invoicingPostalCode" to "00-001",
        )

        mockMvc.put("/api/salon/settings/invoicing") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }
    }

    // r) updateInvoicing_invalidNip_returns400
    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `updateInvoicing invalidNip returns400`() {
        val request = mapOf(
            "taxId" to "123",
        )

        mockMvc.put("/api/salon/settings/invoicing") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }
}

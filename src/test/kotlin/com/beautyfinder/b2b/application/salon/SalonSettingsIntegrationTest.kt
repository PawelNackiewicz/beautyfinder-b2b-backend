package com.beautyfinder.b2b.application.salon

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.salon.SalonSlugAlreadyExistsException
import com.beautyfinder.b2b.infrastructure.salon.SalonCacheService
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
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
import java.time.LocalTime
import java.util.UUID

@SpringBootTest
@Testcontainers
@Transactional
class SalonSettingsIntegrationTest {

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

    @Autowired private lateinit var salonSettingsService: SalonSettingsService
    @Autowired private lateinit var entityManager: EntityManager
    @Autowired private lateinit var salonCacheService: SalonCacheService

    private lateinit var salon: Salon

    @BeforeEach
    fun setUp() {
        salonCacheService.evictAll()
        salon = Salon(name = "Test Salon", slug = "test-${UUID.randomUUID()}")
        entityManager.persist(salon)
        entityManager.flush()
    }

    // a) fullSettingsFlow
    @Test
    fun `fullSettingsFlow`() {
        // Update general settings
        salonSettingsService.updateGeneralSettings(
            salon.id!!,
            UpdateGeneralSettingsRequest(
                name = "Premium Salon",
                timezone = "Europe/Warsaw",
                cancellationWindowHours = 48,
            ),
        )

        // Update opening hours: Mon-Fri 9-18, Sat 9-14, Sun closed
        val openingHoursRequests = listOf(
            UpdateOpeningHoursRequest(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.TUESDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.WEDNESDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.THURSDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.FRIDAY, true, LocalTime.of(9, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.SATURDAY, true, LocalTime.of(9, 0), LocalTime.of(14, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.SUNDAY, false),
        )
        salonSettingsService.updateOpeningHours(salon.id!!, openingHoursRequests)

        // Update loyalty settings
        salonSettingsService.updateLoyaltySettings(
            salon.id!!,
            UpdateLoyaltySettingsRequest(
                enabled = true,
                pointsPerVisit = 10,
                redemptionRate = BigDecimal("0.05"),
            ),
        )

        // Verify all fields via getSalonSettings
        val settings = salonSettingsService.getSalonSettings(salon.id!!)

        assertEquals("Premium Salon", settings.name)
        assertEquals("Europe/Warsaw", settings.timezone)
        assertEquals(48, settings.cancellationWindowHours)

        assertEquals(7, settings.openingHours.size)
        val monday = settings.openingHours.first { it.dayOfWeek == DayOfWeek.MONDAY }
        assertEquals(true, monday.isOpen)
        assertEquals(LocalTime.of(9, 0), monday.openTime)
        assertEquals(LocalTime.of(18, 0), monday.closeTime)

        val saturday = settings.openingHours.first { it.dayOfWeek == DayOfWeek.SATURDAY }
        assertEquals(true, saturday.isOpen)
        assertEquals(LocalTime.of(14, 0), saturday.closeTime)

        val sunday = settings.openingHours.first { it.dayOfWeek == DayOfWeek.SUNDAY }
        assertEquals(false, sunday.isOpen)

        assertEquals(true, settings.loyalty.enabled)
        assertEquals(10, settings.loyalty.pointsPerVisit)
        assertEquals(BigDecimal("0.05"), settings.loyalty.redemptionRate)
    }

    // b) openingHoursAtomicReplace
    @Test
    fun `openingHoursAtomicReplace`() {
        // Save 5 days
        val fiveDays = listOf(
            UpdateOpeningHoursRequest(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.TUESDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.WEDNESDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.THURSDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.FRIDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
        )
        val savedFive = salonSettingsService.updateOpeningHours(salon.id!!, fiveDays)
        assertEquals(5, savedFive.size)

        // Save all 7 days - should replace, not append
        val sevenDays = listOf(
            UpdateOpeningHoursRequest(DayOfWeek.MONDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.TUESDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.WEDNESDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.THURSDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.FRIDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.SATURDAY, true, LocalTime.of(10, 0), LocalTime.of(14, 0)),
            UpdateOpeningHoursRequest(DayOfWeek.SUNDAY, false),
        )
        val savedSeven = salonSettingsService.updateOpeningHours(salon.id!!, sevenDays)
        assertEquals(7, savedSeven.size)

        // Verify exactly 7 records via settings
        val settings = salonSettingsService.getSalonSettings(salon.id!!)
        assertEquals(7, settings.openingHours.size)
    }

    // c) loyaltyPointsCalculation_afterSettingsUpdate
    @Test
    fun `loyaltyPointsCalculation afterSettingsUpdate`() {
        // Enable loyalty with pointsPerVisit=20
        salonSettingsService.updateLoyaltySettings(
            salon.id!!,
            UpdateLoyaltySettingsRequest(
                enabled = true,
                pointsPerVisit = 20,
                redemptionRate = BigDecimal("0.05"),
            ),
        )

        val loyaltyConfig = salonSettingsService.getLoyaltyConfigForAppointment(salon.id!!)
        assertNotNull(loyaltyConfig)
        assertEquals(20, loyaltyConfig!!.calculatePointsForVisit(BigDecimal("150")))
    }

    // d) cacheInvalidation_afterUpdate
    @Test
    fun `cacheInvalidation afterUpdate`() {
        // First call fills cache
        val initialSettings = salonSettingsService.getSalonSettings(salon.id!!)
        assertEquals("Test Salon", initialSettings.name)

        // Verify cache is populated
        val cachedValue = salonCacheService.get(salon.id!!)
        assertNotNull(cachedValue)
        assertEquals("Test Salon", cachedValue!!.name)

        // Update evicts cache
        salonSettingsService.updateGeneralSettings(
            salon.id!!,
            UpdateGeneralSettingsRequest(name = "Renamed Salon"),
        )

        // Second call returns new data (cache was evicted and re-populated)
        val updatedSettings = salonSettingsService.getSalonSettings(salon.id!!)
        assertEquals("Renamed Salon", updatedSettings.name)
    }

    // e) slugUniqueness_acrossSalons
    @Test
    fun `slugUniqueness acrossSalons`() {
        val slug1 = salon.slug

        // Create second salon
        val salon2 = Salon(name = "Another Salon", slug = "another-${UUID.randomUUID()}")
        entityManager.persist(salon2)
        entityManager.flush()

        // Try to set salon2's slug to salon1's slug
        assertThrows(SalonSlugAlreadyExistsException::class.java) {
            salonSettingsService.updateGeneralSettings(
                salon2.id!!,
                UpdateGeneralSettingsRequest(slug = slug1),
            )
        }
    }
}

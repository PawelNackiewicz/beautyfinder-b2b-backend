package com.beautyfinder.b2b.infrastructure.salon

import com.beautyfinder.b2b.application.salon.AddressDto
import com.beautyfinder.b2b.application.salon.BookingPolicyDto
import com.beautyfinder.b2b.application.salon.InvoicingDto
import com.beautyfinder.b2b.application.salon.LoyaltySettingsDto
import com.beautyfinder.b2b.application.salon.SalonSettingsDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class SalonCacheServiceTest {

    private lateinit var cacheService: SalonCacheService

    @BeforeEach
    fun setUp() {
        cacheService = SalonCacheService()
    }

    @Test
    fun `put_andGet_returnsCachedValue`() {
        // given
        val salonId = UUID.randomUUID()
        val dto = buildSettingsDto(salonId)

        // when
        cacheService.put(salonId, dto)
        val result = cacheService.get(salonId)

        // then
        assertNotNull(result)
        assertEquals(dto, result)
    }

    @Test
    fun `get_expiredEntry_returnsNull`() {
        // Test CachedEntry.isExpired directly with a past cachedAt
        val dto = buildSettingsDto(UUID.randomUUID())

        // Entry cached 10 minutes ago with 5-minute TTL should be expired
        val expiredEntry = CachedEntry(
            value = dto,
            cachedAt = Instant.now().minusSeconds(600), // 10 minutes ago
            ttlSeconds = 300, // 5-minute TTL
        )
        assertTrue(expiredEntry.isExpired)

        // Entry cached just now should NOT be expired
        val freshEntry = CachedEntry(
            value = dto,
            cachedAt = Instant.now(),
            ttlSeconds = 300,
        )
        assertFalse(freshEntry.isExpired)

        // Additionally verify the cache service returns null for an expired entry
        // by using reflection to inject an expired entry directly
        val salonId = UUID.randomUUID()
        val cacheField = SalonCacheService::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = cacheField.get(cacheService) as java.util.concurrent.ConcurrentHashMap<UUID, CachedEntry<SalonSettingsDto>>
        cache[salonId] = expiredEntry

        val result = cacheService.get(salonId)
        assertNull(result)
    }

    @Test
    fun `evict_removesEntry`() {
        // given
        val salonId = UUID.randomUUID()
        val dto = buildSettingsDto(salonId)
        cacheService.put(salonId, dto)

        // when
        cacheService.evict(salonId)

        // then
        assertNull(cacheService.get(salonId))
    }

    @Test
    fun `get_nonExistent_returnsNull`() {
        // given
        val salonId = UUID.randomUUID()

        // when
        val result = cacheService.get(salonId)

        // then
        assertNull(result)
    }

    @Test
    fun `put_overwritesExisting`() {
        // given
        val salonId = UUID.randomUUID()
        val dto1 = buildSettingsDto(salonId, name = "First Salon")
        val dto2 = buildSettingsDto(salonId, name = "Second Salon")

        // when
        cacheService.put(salonId, dto1)
        cacheService.put(salonId, dto2)
        val result = cacheService.get(salonId)

        // then
        assertNotNull(result)
        assertEquals("Second Salon", result!!.name)
    }

    @Test
    fun `concurrentAccess_noDataRace`() {
        // given
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val failed = AtomicBoolean(false)
        val salonIds = (1..5).map { UUID.randomUUID() }

        // when - 10 threads doing parallel get/put/evict
        repeat(threadCount) { threadIndex ->
            executor.submit {
                try {
                    for (i in 0 until 100) {
                        val salonId = salonIds[i % salonIds.size]
                        val dto = buildSettingsDto(salonId, name = "Salon-$threadIndex-$i")

                        cacheService.put(salonId, dto)
                        cacheService.get(salonId) // may return null if another thread evicted
                        if (i % 3 == 0) {
                            cacheService.evict(salonId)
                        }
                    }
                } catch (e: Exception) {
                    failed.set(true)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then - no ConcurrentModificationException or other errors
        assertFalse(failed.get(), "Concurrent access caused an exception")
    }

    // --- Helper ---

    private fun buildSettingsDto(
        salonId: UUID = UUID.randomUUID(),
        name: String = "Test Salon",
    ): SalonSettingsDto = SalonSettingsDto(
        id = salonId,
        name = name,
        slug = "test-salon",
        status = "ACTIVE",
        logoUrl = null,
        coverImageUrl = null,
        description = "A test salon",
        websiteUrl = "https://test-salon.com",
        phone = "+48123456789",
        email = "salon@test.com",
        address = AddressDto(
            street = "Test Street 1",
            city = "Warsaw",
            postalCode = "00-001",
            country = "PL",
            latitude = null,
            longitude = null,
            formatted = "Test Street 1, 00-001 Warsaw",
        ),
        timezone = "Europe/Warsaw",
        currency = "PLN",
        cancellationWindowHours = 24,
        bookingWindowDays = 60,
        slotIntervalMinutes = 15,
        defaultAppointmentBufferMinutes = 0,
        invoicing = InvoicingDto(
            invoicingName = "Test Salon Sp. z o.o.",
            taxId = "1234567890",
            street = "Invoice Street 1",
            city = "Warsaw",
            postalCode = "00-002",
            bankAccountNumber = "PL12345678901234567890123456",
            footerNotes = "Thank you for your business",
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
}

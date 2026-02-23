package com.beautyfinder.b2b.application.service

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.service.ServiceStatus
import com.beautyfinder.b2b.domain.service.VariantStatus
import com.beautyfinder.b2b.infrastructure.service.ServiceCacheService
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
import java.util.UUID

@SpringBootTest
@Testcontainers
@Transactional
class ServiceIntegrationTest {

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
    private lateinit var serviceService: ServiceService

    @Autowired
    private lateinit var cacheService: ServiceCacheService

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var salon: Salon
    private lateinit var salonB: Salon

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Test Salon", slug = "test-${UUID.randomUUID()}")
        entityManager.persist(salon)
        salonB = Salon(name = "Other Salon", slug = "other-${UUID.randomUUID()}")
        entityManager.persist(salonB)
        entityManager.flush()
    }

    @Test
    fun `fullServiceFlow - create, add variants, archive`() {
        val salonId = salon.id!!

        // Create category
        val category = serviceService.createCategory(
            CreateCategoryRequest(name = "Włosy", colorHex = "#FF0000"),
            salonId,
        )
        assertNotNull(category.id)

        // Create service
        val service = serviceService.createService(
            CreateServiceRequest(
                name = "Strzyżenie",
                category = "Włosy",
                categoryId = category.id,
            ),
            salonId,
        )
        assertNotNull(service.id)
        assertEquals(ServiceStatus.ACTIVE, service.status)

        // Create variants
        val variant1 = serviceService.createVariant(
            service.id,
            CreateVariantRequest(name = "Standard", durationMinutes = 30, price = BigDecimal("60.00")),
            salonId,
        )
        val variant2 = serviceService.createVariant(
            service.id,
            CreateVariantRequest(name = "Premium", durationMinutes = 45, price = BigDecimal("90.00")),
            salonId,
        )

        // Get service and check variants
        val fetched = serviceService.getService(service.id, salonId)
        assertEquals(2, fetched.variants.size)
        assertEquals(VariantStatus.ACTIVE, fetched.variants[0].status)

        // Archive service
        serviceService.archiveService(service.id, salonId)
        entityManager.flush()
        entityManager.clear()

        val archived = serviceService.getService(service.id, salonId)
        assertEquals(ServiceStatus.ARCHIVED, archived.status)
        archived.variants.forEach {
            assertEquals(VariantStatus.INACTIVE, it.status)
        }
    }

    @Test
    fun `variantCacheConsistency`() {
        val salonId = salon.id!!

        val service = serviceService.createService(
            CreateServiceRequest(name = "Test", category = "Cat"),
            salonId,
        )

        val variant = serviceService.createVariant(
            service.id,
            CreateVariantRequest(name = "V1", durationMinutes = 30, price = BigDecimal("50.00")),
            salonId,
        )

        // Cache miss -> fills cache
        val fromDb = serviceService.getVariant(variant.id, salonId)
        assertEquals("V1", fromDb.name)

        // Cache hit
        val cached = cacheService.getVariant(variant.id)
        assertNotNull(cached)

        // Update evicts cache
        serviceService.updateVariant(
            variant.id,
            UpdateVariantRequest(name = "Updated"),
            salonId,
        )
        assertNull(cacheService.getVariant(variant.id))

        // Re-fetch fills cache with fresh data
        val fresh = serviceService.getVariant(variant.id, salonId)
        assertEquals("Updated", fresh.name)
    }

    @Test
    fun `reorderServices persists correctly`() {
        val salonId = salon.id!!

        val s1 = serviceService.createService(CreateServiceRequest(name = "S1", category = "C"), salonId)
        val s2 = serviceService.createService(CreateServiceRequest(name = "S2", category = "C"), salonId)
        val s3 = serviceService.createService(CreateServiceRequest(name = "S3", category = "C"), salonId)

        // Reverse order
        serviceService.reorderServices(salonId, listOf(s3.id, s2.id, s1.id))
        entityManager.flush()
        entityManager.clear()

        val result = serviceService.listServices(salonId, includeInactive = true)
        assertEquals(s3.id, result[0].id)
        assertEquals(s2.id, result[1].id)
        assertEquals(s1.id, result[2].id)
    }

    @Test
    fun `crossSalonIsolation`() {
        val salonAId = salon.id!!
        val salonBId = salonB.id!!

        serviceService.createService(CreateServiceRequest(name = "A1", category = "C"), salonAId)
        serviceService.createService(CreateServiceRequest(name = "A2", category = "C"), salonAId)
        serviceService.createService(CreateServiceRequest(name = "A3", category = "C"), salonAId)

        serviceService.createService(CreateServiceRequest(name = "B1", category = "C"), salonBId)
        serviceService.createService(CreateServiceRequest(name = "B2", category = "C"), salonBId)

        val salonAServices = serviceService.listServices(salonAId, includeInactive = true)
        val salonBServices = serviceService.listServices(salonBId, includeInactive = true)

        assertEquals(3, salonAServices.size)
        assertEquals(2, salonBServices.size)
    }
}

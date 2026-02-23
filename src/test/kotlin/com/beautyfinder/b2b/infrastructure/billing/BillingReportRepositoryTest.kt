package com.beautyfinder.b2b.infrastructure.billing

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.billing.BillingPeriodReport
import com.beautyfinder.b2b.domain.billing.ReportStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BillingReportRepositoryTest {

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
    private lateinit var reportRepository: BillingPeriodReportRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var salon: Salon

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Test Salon", slug = "test-salon-billing-${UUID.randomUUID()}")
        entityManager.persist(salon)
        entityManager.flush()
    }

    private fun buildReport(year: Int, month: Int, status: ReportStatus = ReportStatus.GENERATED): BillingPeriodReport {
        val ym = java.time.YearMonth.of(year, month)
        return BillingPeriodReport(
            salonId = salon.id!!,
            year = year,
            month = month,
            periodStart = ym.atDay(1),
            periodEnd = ym.atEndOfMonth(),
            totalAppointments = 10,
            completedAppointments = 7,
            cancelledAppointments = 2,
            noShowAppointments = 1,
            marketplaceAppointments = 5,
            directAppointments = 2,
            totalRevenue = BigDecimal("650.00"),
            marketplaceRevenue = BigDecimal("450.00"),
            totalCommission = BigDecimal("67.50"),
            subscriptionFee = BigDecimal("99.00"),
            reportStatus = status,
            generatedAt = OffsetDateTime.now(),
        )
    }

    @Test
    fun `findBySalonIdAndYearAndMonth returnsCorrectReport`() {
        reportRepository.save(buildReport(2024, 3))
        entityManager.flush()

        val result = reportRepository.findBySalonIdAndYearAndMonth(salon.id!!, 2024, 3)
        assertNotNull(result)
        assertEquals(2024, result!!.year)
        assertEquals(3, result.month)
    }

    @Test
    fun `findBySalonIdAndYearAndMonth returnsNull whenNotFound`() {
        val result = reportRepository.findBySalonIdAndYearAndMonth(salon.id!!, 2024, 6)
        assertNull(result)
    }

    @Test
    fun `findMonthlyRevenue last6Months returnsOrderedList`() {
        for (m in 1..6) {
            reportRepository.save(buildReport(2024, m))
        }
        entityManager.flush()

        val result = reportRepository.findMonthlyRevenue(salon.id!!, 2024, 1)
        assertEquals(6, result.size)
        // Ordered desc
        assertEquals(6, result.first().month)
        assertEquals(1, result.last().month)
    }

    @Test
    fun `uniqueConstraint duplicateMonthReport throwsException`() {
        reportRepository.save(buildReport(2024, 3))
        entityManager.flush()

        assertThrows<DataIntegrityViolationException> {
            reportRepository.save(buildReport(2024, 3))
            entityManager.flush()
        }
    }

    @Test
    fun `findAllByReportStatus filtersCorrectly`() {
        reportRepository.save(buildReport(2024, 1, ReportStatus.GENERATED))
        reportRepository.save(buildReport(2024, 2, ReportStatus.INVOICED))
        reportRepository.save(buildReport(2024, 3, ReportStatus.GENERATED))
        entityManager.flush()

        val generated = reportRepository.findAllByReportStatus(ReportStatus.GENERATED)
        assertEquals(2, generated.size)

        val invoiced = reportRepository.findAllByReportStatus(ReportStatus.INVOICED)
        assertEquals(1, invoiced.size)
    }
}

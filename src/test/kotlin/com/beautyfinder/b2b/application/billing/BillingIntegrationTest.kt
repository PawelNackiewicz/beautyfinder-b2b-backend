package com.beautyfinder.b2b.application.billing

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.service.Service
import com.beautyfinder.b2b.domain.service.ServiceVariant
import com.beautyfinder.b2b.domain.User
import com.beautyfinder.b2b.domain.UserRole
import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.billing.InvoiceStatus
import com.beautyfinder.b2b.domain.billing.SalonSubscription
import com.beautyfinder.b2b.domain.billing.SubscriptionPlan
import com.beautyfinder.b2b.domain.billing.SubscriptionStatus
import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.domain.employee.Employee
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@Testcontainers
@Transactional
class BillingIntegrationTest {

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
    private lateinit var billingReportService: BillingReportService

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var salon: Salon
    private lateinit var employee: Employee
    private lateinit var client: Client
    private lateinit var variant: ServiceVariant

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Test Billing Salon", slug = "billing-test-${UUID.randomUUID()}")
        entityManager.persist(salon)

        val user = User(salonId = salon.id!!, role = UserRole.OWNER, email = "billing-${UUID.randomUUID()}@test.com", passwordHash = "hash")
        entityManager.persist(user)

        employee = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "Test Employee")
        entityManager.persist(employee)

        val service = Service(salonId = salon.id!!, name = "Haircut", category = "Hair")
        entityManager.persist(service)

        variant = ServiceVariant(serviceId = service.id!!, salonId = salon.id!!, name = "Standard", durationMinutes = 60, price = BigDecimal("100.00"))
        entityManager.persist(variant)

        client = Client(salonId = salon.id!!, firstName = "Jane", lastName = "Doe", phone = "+48123456789")
        entityManager.persist(client)

        // Create subscription: BASIC plan, 99 PLN, 15% commission
        val subscription = SalonSubscription(
            salonId = salon.id!!,
            plan = SubscriptionPlan.BASIC,
            monthlyFee = BigDecimal("99.00"),
            commissionRate = BigDecimal("0.1500"),
            validFrom = LocalDate.of(2024, 1, 1),
            status = SubscriptionStatus.ACTIVE,
        )
        entityManager.persist(subscription)

        // Create 5 COMPLETED appointments in previous month (2024-02)
        // 3 MARKETPLACE
        createAppointment(1, AppointmentSource.MARKETPLACE, BigDecimal("100.00"), BigDecimal("15.00"))
        createAppointment(2, AppointmentSource.MARKETPLACE, BigDecimal("150.00"), BigDecimal("22.50"))
        createAppointment(3, AppointmentSource.MARKETPLACE, BigDecimal("200.00"), BigDecimal("30.00"))
        // 2 DIRECT
        createAppointment(4, AppointmentSource.DIRECT, BigDecimal("80.00"), null)
        createAppointment(5, AppointmentSource.DIRECT, BigDecimal("120.00"), null)

        entityManager.flush()
        entityManager.clear()
    }

    private fun createAppointment(day: Int, source: AppointmentSource, price: BigDecimal, commission: BigDecimal?) {
        val appointment = Appointment(
            salonId = salon.id!!,
            clientId = client.id!!,
            employeeId = employee.id!!,
            variantId = variant.id!!,
            startAt = OffsetDateTime.of(2024, 2, day, 10, 0, 0, 0, ZoneOffset.UTC),
            endAt = OffsetDateTime.of(2024, 2, day, 11, 0, 0, 0, ZoneOffset.UTC),
            status = AppointmentStatus.COMPLETED,
            source = source,
            finalPrice = price,
            commissionValue = commission,
        )
        entityManager.persist(appointment)
    }

    @Test
    fun `fullBillingCycle generateReport thenInvoice`() {
        // Step 1: Generate monthly report
        val report = billingReportService.generateMonthlyReport(salon.id!!, 2024, 2)

        assertEquals(BigDecimal("650.00"), report.totalRevenue)
        assertEquals(BigDecimal("67.50"), report.totalCommission)
        assertEquals(BigDecimal("99.00"), report.subscriptionFee)
        assertEquals(5, report.completedAppointments)
        assertEquals(3, report.marketplaceAppointments)
        assertEquals(2, report.directAppointments)

        // Step 2: Generate invoice
        val invoice = billingReportService.generateInvoice(report.id, salon.id!!)

        val expectedNet = BigDecimal("166.50") // 99 + 67.5
        val expectedVat = BigDecimal("38.30") // 166.50 * 0.23 = 38.295 → 38.30 HALF_UP
        val expectedGross = BigDecimal("204.80") // 166.50 + 38.30

        assertEquals(expectedNet, invoice.netAmount)
        assertEquals(expectedVat, invoice.vatAmount)
        assertEquals(expectedGross, invoice.grossAmount)
        assertEquals(InvoiceStatus.ISSUED, invoice.status)
        assertEquals(2, invoice.lineItems.size)

        // Step 3: Mark as paid
        billingReportService.markInvoiceAsPaid(invoice.id, salon.id!!)
        val paidInvoice = billingReportService.getInvoice(invoice.id, salon.id!!)
        assertEquals(InvoiceStatus.PAID, paidInvoice.status)
        assertNotNull(paidInvoice.paidAt)
    }

    @Test
    fun `getReportDetails correctEmployeeBreakdown`() {
        val report = billingReportService.generateMonthlyReport(salon.id!!, 2024, 2)
        val details = billingReportService.getReportDetails(report.id, salon.id!!)

        assertEquals(1, details.perEmployeeStats.size)
        val empStats = details.perEmployeeStats.first()
        assertEquals(employee.id, empStats.employeeId)
        assertEquals("Test Employee", empStats.employeeName)
        assertEquals(5, empStats.completedAppointments)
        assertEquals(BigDecimal("650.00"), empStats.revenue.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun `getRevenueStats currentMonth calculatesProjection`() {
        // This test validates that the stats endpoint works without errors
        val stats = billingReportService.getRevenueStats(salon.id!!, StatsPeriod.CURRENT_MONTH)

        assertNotNull(stats)
        assertNotNull(stats.projectedMonthRevenue)
        assertNotNull(stats.avgDailyRevenue)
    }
}

package com.beautyfinder.b2b.api.billing

import com.beautyfinder.b2b.application.billing.BillingPeriodReportDto
import com.beautyfinder.b2b.application.billing.BillingReportService
import com.beautyfinder.b2b.application.billing.InvoiceDto
import com.beautyfinder.b2b.application.billing.InvoiceLineItemDto
import com.beautyfinder.b2b.application.billing.InvoiceSummaryDto
import com.beautyfinder.b2b.application.billing.MonthlyRevenueDto
import com.beautyfinder.b2b.application.billing.RevenueStatsDto
import com.beautyfinder.b2b.application.billing.StatsPeriod
import com.beautyfinder.b2b.config.JwtAuthenticationFilter
import com.beautyfinder.b2b.config.JwtService
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.billing.BillingDomainException
import com.beautyfinder.b2b.domain.billing.InvoiceNotFoundException
import com.beautyfinder.b2b.domain.billing.InvoiceStatus
import com.beautyfinder.b2b.domain.billing.InvoiceType
import com.beautyfinder.b2b.domain.billing.LineItemType
import com.beautyfinder.b2b.domain.billing.ReportStatus
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@WebMvcTest(BillingController::class)
@Import(BillingControllerTest.MockConfig::class, com.beautyfinder.b2b.config.SecurityConfig::class)
class BillingControllerTest {

    @TestConfiguration
    class MockConfig {
        @Bean
        fun billingReportService(): BillingReportService = io.mockk.mockk()

        @Bean
        fun jwtService(): JwtService = io.mockk.mockk()

        @Bean
        fun jwtAuthenticationFilter(jwtService: JwtService): JwtAuthenticationFilter =
            JwtAuthenticationFilter(jwtService)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var billingReportService: BillingReportService

    private val salonId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mockkObject(TenantContext)
        every { TenantContext.getSalonId() } returns salonId
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TenantContext)
    }

    private fun buildReportDto() = BillingPeriodReportDto(
        id = UUID.randomUUID(),
        salonId = salonId,
        year = 2024,
        month = 3,
        periodStart = LocalDate.of(2024, 3, 1),
        periodEnd = LocalDate.of(2024, 3, 31),
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
        totalDue = BigDecimal("166.50"),
        reportStatus = ReportStatus.GENERATED,
        generatedAt = OffsetDateTime.now(),
        invoiceId = null,
    )

    private fun buildInvoiceDto() = InvoiceDto(
        id = UUID.randomUUID(),
        invoiceNumber = "BF/2024/03/001",
        type = InvoiceType.COMBINED,
        periodStart = LocalDate.of(2024, 3, 1),
        periodEnd = LocalDate.of(2024, 3, 31),
        issuedAt = OffsetDateTime.of(2024, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC),
        dueAt = OffsetDateTime.of(2024, 4, 15, 10, 0, 0, 0, ZoneOffset.UTC),
        status = InvoiceStatus.ISSUED,
        netAmount = BigDecimal("166.50"),
        vatRate = BigDecimal("0.23"),
        vatAmount = BigDecimal("38.30"),
        grossAmount = BigDecimal("204.80"),
        currency = "PLN",
        paidAt = null,
        notes = null,
        lineItems = listOf(
            InvoiceLineItemDto(
                id = UUID.randomUUID(),
                description = "Subscription fee",
                quantity = BigDecimal.ONE,
                unitPrice = BigDecimal("99.00"),
                netAmount = BigDecimal("99.00"),
                itemType = LineItemType.SUBSCRIPTION_FEE,
            )
        ),
    )

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getReport existingReport returns200`() {
        every { billingReportService.getMonthlyReport(salonId, 2024, 3) } returns buildReportDto()

        mockMvc.get("/api/billing/report?year=2024&month=3")
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.totalAppointments") { value(10) } }
            .andExpect { jsonPath("$.reportStatus") { value("GENERATED") } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getReport invalidMonth returns400`() {
        mockMvc.get("/api/billing/report?year=2024&month=13")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getReport generatesIfMissing returns200`() {
        every { billingReportService.getMonthlyReport(salonId, 2024, 3) } returns buildReportDto()

        mockMvc.get("/api/billing/report?year=2024&month=3")
            .andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `createInvoice returns201WithInvoiceNumber`() {
        val reportId = UUID.randomUUID()
        every { billingReportService.generateInvoice(reportId, salonId) } returns buildInvoiceDto()

        mockMvc.post("/api/billing/report/$reportId/invoice")
            .andExpect { status { isCreated() } }
            .andExpect { jsonPath("$.invoiceNumber") { value("BF/2024/03/001") } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `createInvoice alreadyInvoiced returns409`() {
        val reportId = UUID.randomUUID()
        every { billingReportService.generateInvoice(reportId, salonId) } throws
            BillingDomainException("Report already invoiced: $reportId")

        mockMvc.post("/api/billing/report/$reportId/invoice")
            .andExpect { status { isConflict() } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `listInvoices returns200WithPage`() {
        val invoiceSummary = InvoiceSummaryDto(
            id = UUID.randomUUID(),
            invoiceNumber = "BF/2024/03/001",
            type = InvoiceType.COMBINED,
            issuedAt = OffsetDateTime.now(),
            dueAt = OffsetDateTime.now().plusDays(14),
            status = InvoiceStatus.ISSUED,
            grossAmount = BigDecimal("204.80"),
            currency = "PLN",
            paidAt = null,
        )
        every { billingReportService.listInvoices(salonId, any()) } returns
            PageImpl(listOf(invoiceSummary), PageRequest.of(0, 20), 1)

        mockMvc.get("/api/billing/invoices")
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.content[0].invoiceNumber") { value("BF/2024/03/001") } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getInvoice notFound returns404`() {
        val invoiceId = UUID.randomUUID()
        every { billingReportService.getInvoice(invoiceId, salonId) } throws InvoiceNotFoundException(invoiceId)

        mockMvc.get("/api/billing/invoices/$invoiceId")
            .andExpect { status { isNotFound() } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `markInvoiceAsPaid returns204`() {
        val invoiceId = UUID.randomUUID()
        every { billingReportService.markInvoiceAsPaid(invoiceId, salonId) } returns Unit

        mockMvc.patch("/api/billing/invoices/$invoiceId/mark-paid")
            .andExpect { status { isNoContent() } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getStats defaultPeriod returns200`() {
        every { billingReportService.getRevenueStats(salonId, StatsPeriod.CURRENT_MONTH) } returns RevenueStatsDto(
            totalRevenue = BigDecimal("1000.00"),
            totalCommission = BigDecimal("150.00"),
            avgDailyRevenue = BigDecimal("50.00"),
            revenueByMonth = listOf(MonthlyRevenueDto(2024, 3, BigDecimal("1000.00"), BigDecimal("150.00"), 20)),
            revenueGrowth = BigDecimal("25.00"),
            projectedMonthRevenue = BigDecimal("2000.00"),
        )

        mockMvc.get("/api/billing/stats")
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.totalRevenue") { value(1000.00) } }
    }

    @Test
    @WithMockUser(roles = ["OWNER"])
    fun `getStats customPeriod returns200`() {
        every { billingReportService.getRevenueStats(salonId, StatsPeriod.LAST_30_DAYS) } returns RevenueStatsDto(
            totalRevenue = BigDecimal("3000.00"),
            totalCommission = BigDecimal("450.00"),
            avgDailyRevenue = BigDecimal("100.00"),
            revenueByMonth = emptyList(),
            revenueGrowth = BigDecimal("10.00"),
            projectedMonthRevenue = BigDecimal("3100.00"),
        )

        mockMvc.get("/api/billing/stats?period=LAST_30_DAYS")
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.totalRevenue") { value(3000.00) } }
    }

    @Test
    @WithMockUser(roles = ["EMPLOYEE"])
    fun `employeeRole returns403`() {
        mockMvc.get("/api/billing/report?year=2024&month=3")
            .andExpect { status { isForbidden() } }
    }
}

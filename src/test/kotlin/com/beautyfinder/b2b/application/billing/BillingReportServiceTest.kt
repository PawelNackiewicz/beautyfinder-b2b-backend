package com.beautyfinder.b2b.application.billing

import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.billing.BillingPeriodReport
import com.beautyfinder.b2b.domain.billing.Invoice
import com.beautyfinder.b2b.domain.billing.InvoiceAlreadyPaidException
import com.beautyfinder.b2b.domain.billing.InvoiceStatus
import com.beautyfinder.b2b.domain.billing.InvoiceType
import com.beautyfinder.b2b.domain.billing.NoActiveSubscriptionException
import com.beautyfinder.b2b.domain.billing.ReportAlreadyGeneratedException
import com.beautyfinder.b2b.domain.billing.ReportStatus
import com.beautyfinder.b2b.domain.billing.SalonSubscription
import com.beautyfinder.b2b.domain.billing.SubscriptionPlan
import com.beautyfinder.b2b.domain.billing.SubscriptionStatus
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import com.beautyfinder.b2b.infrastructure.ClientRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.service.ServiceRepository
import com.beautyfinder.b2b.infrastructure.billing.BillingPeriodReportRepository
import com.beautyfinder.b2b.infrastructure.billing.InvoiceLineItemRepository
import com.beautyfinder.b2b.infrastructure.billing.InvoiceRepository
import com.beautyfinder.b2b.infrastructure.billing.SalonSubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class BillingReportServiceTest {

    private val reportRepository = mockk<BillingPeriodReportRepository>(relaxed = true)
    private val invoiceRepository = mockk<InvoiceRepository>(relaxed = true)
    private val lineItemRepository = mockk<InvoiceLineItemRepository>(relaxed = true)
    private val subscriptionRepository = mockk<SalonSubscriptionRepository>(relaxed = true)
    private val appointmentRepository = mockk<AppointmentRepository>(relaxed = true)
    private val employeeRepository = mockk<EmployeeRepository>(relaxed = true)
    private val clientRepository = mockk<ClientRepository>(relaxed = true)
    private val serviceRepository = mockk<ServiceRepository>(relaxed = true)

    private val service = BillingReportService(
        reportRepository = reportRepository,
        invoiceRepository = invoiceRepository,
        lineItemRepository = lineItemRepository,
        subscriptionRepository = subscriptionRepository,
        appointmentRepository = appointmentRepository,
        employeeRepository = employeeRepository,
        clientRepository = clientRepository,
        serviceRepository = serviceRepository,
    )

    private val salonId = UUID.randomUUID()

    // -- Fixtures --

    private fun buildSubscription(
        monthlyFee: BigDecimal = BigDecimal("99.00"),
        commissionRate: BigDecimal = BigDecimal("0.15"),
    ) = SalonSubscription(
        salonId = salonId,
        plan = SubscriptionPlan.BASIC,
        monthlyFee = monthlyFee,
        commissionRate = commissionRate,
        validFrom = LocalDate.of(2024, 1, 1),
        status = SubscriptionStatus.ACTIVE,
    )

    private fun buildAppointmentList(
        count: Int,
        source: AppointmentSource = AppointmentSource.DIRECT,
        status: AppointmentStatus = AppointmentStatus.COMPLETED,
        price: BigDecimal = BigDecimal("100.00"),
        commission: BigDecimal? = null,
    ): List<Appointment> = (1..count).map {
        Appointment(
            salonId = salonId,
            clientId = UUID.randomUUID(),
            employeeId = UUID.randomUUID(),
            variantId = UUID.randomUUID(),
            startAt = OffsetDateTime.of(2024, 3, it.coerceAtMost(28), 10, 0, 0, 0, ZoneOffset.UTC),
            endAt = OffsetDateTime.of(2024, 3, it.coerceAtMost(28), 11, 0, 0, 0, ZoneOffset.UTC),
            status = status,
            source = source,
            finalPrice = if (status == AppointmentStatus.COMPLETED) price else null,
            commissionValue = commission,
        )
    }

    private fun buildReport(
        status: ReportStatus = ReportStatus.GENERATED,
        totalCommission: BigDecimal = BigDecimal("67.50"),
        subscriptionFee: BigDecimal = BigDecimal("99.00"),
    ): BillingPeriodReport {
        val report = BillingPeriodReport(
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
            totalCommission = totalCommission,
            subscriptionFee = subscriptionFee,
            reportStatus = status,
            generatedAt = OffsetDateTime.now(),
        )
        // Use reflection to set id for testing
        val idField = report.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(report, UUID.randomUUID())
        return report
    }

    private fun buildInvoice(
        status: InvoiceStatus = InvoiceStatus.ISSUED,
    ): Invoice {
        val invoice = Invoice(
            salonId = salonId,
            invoiceNumber = "BF/2024/03/001",
            type = InvoiceType.COMBINED,
            periodStart = LocalDate.of(2024, 3, 1),
            periodEnd = LocalDate.of(2024, 3, 31),
            issuedAt = OffsetDateTime.now(),
            dueAt = OffsetDateTime.now().plusDays(14),
            status = status,
            netAmount = BigDecimal("166.50"),
            vatRate = BigDecimal("0.23"),
            vatAmount = BigDecimal("38.30"),
            grossAmount = BigDecimal("204.80"),
        )
        val idField = invoice.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(invoice, UUID.randomUUID())
        return invoice
    }

    // -- Tests --

    @Test
    fun `generateMonthlyReport success calculatesCorrectAggregates`() {
        val marketplaceCompleted = buildAppointmentList(5, AppointmentSource.MARKETPLACE, AppointmentStatus.COMPLETED, BigDecimal("100.00"), BigDecimal("15.00"))
        val directCompleted = buildAppointmentList(2, AppointmentSource.DIRECT, AppointmentStatus.COMPLETED, BigDecimal("100.00"))
        val cancelled = buildAppointmentList(2, status = AppointmentStatus.CANCELLED, price = BigDecimal.ZERO)
        val noShow = buildAppointmentList(1, status = AppointmentStatus.NO_SHOW, price = BigDecimal.ZERO)

        val allAppointments = marketplaceCompleted + directCompleted + cancelled + noShow
        val subscription = buildSubscription()

        every { reportRepository.findBySalonIdAndYearAndMonth(salonId, 2024, 3) } returns null
        every { appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, any(), any()) } returns allAppointments
        every { subscriptionRepository.findBySalonIdAndStatus(salonId, SubscriptionStatus.ACTIVE) } returns subscription

        val reportSlot = slot<BillingPeriodReport>()
        every { reportRepository.save(capture(reportSlot)) } answers {
            val r = reportSlot.captured
            val idField = r.javaClass.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(r, UUID.randomUUID())
            r
        }

        val result = service.generateMonthlyReport(salonId, 2024, 3)

        assertEquals(10, result.totalAppointments)
        assertEquals(7, result.completedAppointments)
        assertEquals(2, result.cancelledAppointments)
        assertEquals(1, result.noShowAppointments)
        assertEquals(5, result.marketplaceAppointments)
        assertEquals(2, result.directAppointments)
        assertEquals(BigDecimal("700.00"), result.totalRevenue)
        assertEquals(BigDecimal("500.00"), result.marketplaceRevenue)
        assertEquals(BigDecimal("75.00"), result.totalCommission)
        assertEquals(BigDecimal("99.00"), result.subscriptionFee)
        assertEquals(ReportStatus.GENERATED, result.reportStatus)
    }

    @Test
    fun `generateMonthlyReport alreadyExists throwsException`() {
        every { reportRepository.findBySalonIdAndYearAndMonth(salonId, 2024, 3) } returns buildReport()

        assertThrows<ReportAlreadyGeneratedException> {
            service.generateMonthlyReport(salonId, 2024, 3)
        }
    }

    @Test
    fun `generateMonthlyReport noSubscription throwsException`() {
        every { reportRepository.findBySalonIdAndYearAndMonth(salonId, 2024, 3) } returns null
        every { appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, any(), any()) } returns emptyList()
        every { subscriptionRepository.findBySalonIdAndStatus(salonId, SubscriptionStatus.ACTIVE) } returns null

        assertThrows<NoActiveSubscriptionException> {
            service.generateMonthlyReport(salonId, 2024, 3)
        }
    }

    @Test
    fun `generateMonthlyReport marketplaceCommission 15percent`() {
        val appointments = buildAppointmentList(1, AppointmentSource.MARKETPLACE, AppointmentStatus.COMPLETED, BigDecimal("200.00"), BigDecimal("30.00"))

        every { reportRepository.findBySalonIdAndYearAndMonth(salonId, 2024, 3) } returns null
        every { appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, any(), any()) } returns appointments
        every { subscriptionRepository.findBySalonIdAndStatus(salonId, SubscriptionStatus.ACTIVE) } returns buildSubscription()

        val reportSlot = slot<BillingPeriodReport>()
        every { reportRepository.save(capture(reportSlot)) } answers {
            val r = reportSlot.captured
            val idField = r.javaClass.superclass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(r, UUID.randomUUID())
            r
        }

        val result = service.generateMonthlyReport(salonId, 2024, 3)

        assertEquals(BigDecimal("30.00"), result.totalCommission)
    }

    @Test
    fun `generateInvoice combinedType correctLineItems`() {
        val report = buildReport(
            status = ReportStatus.GENERATED,
            totalCommission = BigDecimal("150.00"),
            subscriptionFee = BigDecimal("99.00"),
        )
        val subscription = buildSubscription()

        every { reportRepository.findByIdAndSalonId(report.id!!, salonId) } returns report
        every { subscriptionRepository.findBySalonIdAndStatus(salonId, SubscriptionStatus.ACTIVE) } returns subscription
        every { invoiceRepository.countInvoicesInMonth(salonId, 2024, 3) } returns 0
        every { invoiceRepository.save(any<Invoice>()) } answers {
            val inv = firstArg<Invoice>()
            setId(inv, UUID.randomUUID())
            inv
        }
        every { lineItemRepository.save(any()) } answers {
            val item = firstArg<com.beautyfinder.b2b.domain.billing.InvoiceLineItem>()
            setId(item, UUID.randomUUID())
            item
        }
        every { reportRepository.save(any<BillingPeriodReport>()) } answers { firstArg() }

        val result = service.generateInvoice(report.id!!, salonId)

        assertEquals(InvoiceType.COMBINED, result.type)
        assertEquals(BigDecimal("249.00"), result.netAmount)
        assertEquals(BigDecimal("57.27"), result.vatAmount)
        assertEquals(BigDecimal("306.27"), result.grossAmount)
        assertEquals(2, result.lineItems.size)
    }

    @Test
    fun `generateInvoice alreadyInvoiced throwsException`() {
        val report = buildReport(status = ReportStatus.INVOICED)

        every { reportRepository.findByIdAndSalonId(report.id!!, salonId) } returns report

        assertThrows<com.beautyfinder.b2b.domain.billing.BillingDomainException> {
            service.generateInvoice(report.id!!, salonId)
        }
    }

    @Test
    fun `markInvoiceAsPaid setsCorrectStatus`() {
        val invoice = buildInvoice(InvoiceStatus.ISSUED)

        every { invoiceRepository.findByIdAndSalonId(invoice.id!!, salonId) } returns invoice
        every { invoiceRepository.save(any<Invoice>()) } answers { firstArg() }

        service.markInvoiceAsPaid(invoice.id!!, salonId)

        assertEquals(InvoiceStatus.PAID, invoice.status)
        assertNotNull(invoice.paidAt)
    }

    @Test
    fun `markInvoiceAsPaid alreadyPaid throwsException`() {
        val invoice = buildInvoice(InvoiceStatus.PAID)

        every { invoiceRepository.findByIdAndSalonId(invoice.id!!, salonId) } returns invoice

        assertThrows<InvoiceAlreadyPaidException> {
            service.markInvoiceAsPaid(invoice.id!!, salonId)
        }
    }

    @Test
    fun `monthlyBillingScheduler generatesForAllSalons`() {
        val scheduler = MonthlyBillingScheduler(service, subscriptionRepository)
        val sub1 = buildSubscription().also { setId(it, UUID.randomUUID()) }
        val sub2 = buildSubscription().also { setId(it, UUID.randomUUID()) }
        val sub3 = buildSubscription().also { setId(it, UUID.randomUUID()) }

        every { subscriptionRepository.findAll() } returns listOf(sub1, sub2, sub3)
        every { reportRepository.findBySalonIdAndYearAndMonth(any(), any(), any()) } returns null
        every { appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(any(), any(), any()) } returns emptyList()
        every { subscriptionRepository.findBySalonIdAndStatus(any(), SubscriptionStatus.ACTIVE) } returns buildSubscription()
        every { reportRepository.save(any()) } answers {
            val r = firstArg<BillingPeriodReport>()
            setId(r, UUID.randomUUID())
            r
        }

        scheduler.generateMonthlyReports()

        verify(exactly = 3) { reportRepository.save(any()) }
    }

    @Test
    fun `monthlyBillingScheduler oneFailure continuesOthers`() {
        val scheduler = MonthlyBillingScheduler(service, subscriptionRepository)
        val salonId1 = UUID.randomUUID()
        val salonId2 = UUID.randomUUID()
        val salonId3 = UUID.randomUUID()

        val sub1 = SalonSubscription(salonId = salonId1, plan = SubscriptionPlan.BASIC, monthlyFee = BigDecimal("99.00"), commissionRate = BigDecimal("0.15"), validFrom = LocalDate.of(2024, 1, 1), status = SubscriptionStatus.ACTIVE).also { setId(it, UUID.randomUUID()) }
        val sub2 = SalonSubscription(salonId = salonId2, plan = SubscriptionPlan.BASIC, monthlyFee = BigDecimal("99.00"), commissionRate = BigDecimal("0.15"), validFrom = LocalDate.of(2024, 1, 1), status = SubscriptionStatus.ACTIVE).also { setId(it, UUID.randomUUID()) }
        val sub3 = SalonSubscription(salonId = salonId3, plan = SubscriptionPlan.BASIC, monthlyFee = BigDecimal("99.00"), commissionRate = BigDecimal("0.15"), validFrom = LocalDate.of(2024, 1, 1), status = SubscriptionStatus.ACTIVE).also { setId(it, UUID.randomUUID()) }

        every { subscriptionRepository.findAll() } returns listOf(sub1, sub2, sub3)

        // Salon 1 and 3 succeed
        every { reportRepository.findBySalonIdAndYearAndMonth(salonId1, any(), any()) } returns null
        every { reportRepository.findBySalonIdAndYearAndMonth(salonId3, any(), any()) } returns null
        every { appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId1, any(), any()) } returns emptyList()
        every { appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId3, any(), any()) } returns emptyList()
        every { subscriptionRepository.findBySalonIdAndStatus(salonId1, SubscriptionStatus.ACTIVE) } returns sub1
        every { subscriptionRepository.findBySalonIdAndStatus(salonId3, SubscriptionStatus.ACTIVE) } returns sub3

        // Salon 2 fails - report already exists
        every { reportRepository.findBySalonIdAndYearAndMonth(salonId2, any(), any()) } returns buildReport()

        every { reportRepository.save(any()) } answers {
            val r = firstArg<BillingPeriodReport>()
            setId(r, UUID.randomUUID())
            r
        }

        scheduler.generateMonthlyReports()

        // Should still save for salon 1 and 3
        verify(exactly = 2) { reportRepository.save(any()) }
    }

    private fun setId(entity: Any, id: UUID) {
        val idField = entity.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}

package com.beautyfinder.b2b.application.billing

import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.billing.BillingPeriodReport
import com.beautyfinder.b2b.domain.billing.Invoice
import com.beautyfinder.b2b.domain.billing.InvoiceAlreadyPaidException
import com.beautyfinder.b2b.domain.billing.InvoiceLineItem
import com.beautyfinder.b2b.domain.billing.InvoiceNotFoundException
import com.beautyfinder.b2b.domain.billing.InvoiceNumber
import com.beautyfinder.b2b.domain.billing.InvoiceStatus
import com.beautyfinder.b2b.domain.billing.InvoiceType
import com.beautyfinder.b2b.domain.billing.LineItemType
import com.beautyfinder.b2b.domain.billing.NoActiveSubscriptionException
import com.beautyfinder.b2b.domain.billing.ReportAlreadyGeneratedException
import com.beautyfinder.b2b.domain.billing.ReportStatus
import com.beautyfinder.b2b.domain.billing.SubscriptionStatus
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import com.beautyfinder.b2b.infrastructure.ClientRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.ServiceRepository
import com.beautyfinder.b2b.infrastructure.billing.BillingPeriodReportRepository
import com.beautyfinder.b2b.infrastructure.billing.InvoiceLineItemRepository
import com.beautyfinder.b2b.infrastructure.billing.InvoiceRepository
import com.beautyfinder.b2b.infrastructure.billing.SalonSubscriptionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

@Service
class BillingReportService(
    private val reportRepository: BillingPeriodReportRepository,
    private val invoiceRepository: InvoiceRepository,
    private val lineItemRepository: InvoiceLineItemRepository,
    private val subscriptionRepository: SalonSubscriptionRepository,
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val clientRepository: ClientRepository,
    private val serviceRepository: ServiceRepository,
) {

    @Transactional
    fun generateMonthlyReport(salonId: UUID, year: Int, month: Int): BillingPeriodReportDto {
        reportRepository.findBySalonIdAndYearAndMonth(salonId, year, month)?.let {
            throw ReportAlreadyGeneratedException(salonId, year, month)
        }

        val yearMonth = YearMonth.of(year, month)
        val periodStart = yearMonth.atDay(1)
        val periodEnd = yearMonth.atEndOfMonth()
        val from = periodStart.atStartOfDay().atOffset(ZoneOffset.UTC)
        val to = periodEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

        val appointments = appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, from, to)

        val completed = appointments.filter { it.status == AppointmentStatus.COMPLETED }
        val cancelled = appointments.count { it.status == AppointmentStatus.CANCELLED }
        val noShow = appointments.count { it.status == AppointmentStatus.NO_SHOW }
        val marketplace = completed.filter { it.source == AppointmentSource.MARKETPLACE }
        val direct = completed.filter { it.source == AppointmentSource.DIRECT }

        val totalRevenue = completed.sumOf { it.finalPrice ?: BigDecimal.ZERO }.setScale(2, RoundingMode.HALF_UP)
        val marketplaceRevenue = marketplace.sumOf { it.finalPrice ?: BigDecimal.ZERO }.setScale(2, RoundingMode.HALF_UP)
        val totalCommission = completed.sumOf { it.commissionValue ?: BigDecimal.ZERO }.setScale(2, RoundingMode.HALF_UP)

        val subscription = subscriptionRepository.findBySalonIdAndStatus(salonId, SubscriptionStatus.ACTIVE)
            ?: throw NoActiveSubscriptionException(salonId)

        val report = reportRepository.save(
            BillingPeriodReport(
                salonId = salonId,
                year = year,
                month = month,
                periodStart = periodStart,
                periodEnd = periodEnd,
                totalAppointments = appointments.size,
                completedAppointments = completed.size,
                cancelledAppointments = cancelled,
                noShowAppointments = noShow,
                marketplaceAppointments = marketplace.size,
                directAppointments = direct.size,
                totalRevenue = totalRevenue,
                marketplaceRevenue = marketplaceRevenue,
                totalCommission = totalCommission,
                subscriptionFee = subscription.monthlyFee,
                reportStatus = ReportStatus.GENERATED,
                generatedAt = OffsetDateTime.now(),
            )
        )

        return report.toDto()
    }

    @Transactional(readOnly = true)
    fun getMonthlyReport(salonId: UUID, year: Int, month: Int): BillingPeriodReportDto {
        val report = reportRepository.findBySalonIdAndYearAndMonth(salonId, year, month)
        return report?.toDto() ?: generateMonthlyReport(salonId, year, month)
    }

    @Transactional(readOnly = true)
    fun listReports(salonId: UUID, pageable: Pageable): Page<BillingPeriodReportSummaryDto> =
        reportRepository.findAllBySalonIdOrderByYearDescMonthDesc(salonId, pageable)
            .map { it.toSummaryDto() }

    @Transactional(readOnly = true)
    fun getReportDetails(reportId: UUID, salonId: UUID): BillingReportDetailsDto {
        val report = reportRepository.findByIdAndSalonId(reportId, salonId)
            ?: throw com.beautyfinder.b2b.domain.billing.BillingDomainException("Report not found: $reportId")

        val from = report.periodStart.atStartOfDay().atOffset(ZoneOffset.UTC)
        val to = report.periodEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

        val employeeProjections = appointmentRepository.findRevenueByEmployee(salonId, from, to)
        val employees = employeeRepository.findAllBySalonId(salonId).associateBy { it.id }
        val perEmployeeStats = employeeProjections.map { p ->
            EmployeeRevenueDto(
                employeeId = p.employeeId,
                employeeName = employees[p.employeeId]?.displayName ?: "Unknown",
                completedAppointments = p.appointmentCount,
                revenue = p.revenue ?: BigDecimal.ZERO,
                commission = p.commission ?: BigDecimal.ZERO,
            )
        }

        val serviceProjections = appointmentRepository.findRevenueByService(salonId, from, to)
        val services = serviceRepository.findAllBySalonId(salonId).associateBy { it.id }
        val perServiceStats = serviceProjections.map { p ->
            ServiceRevenueDto(
                serviceId = p.serviceId,
                serviceName = services[p.serviceId]?.name ?: "Unknown",
                completedAppointments = p.appointmentCount,
                revenue = p.revenue ?: BigDecimal.ZERO,
            )
        }

        val clientProjections = appointmentRepository.findTopClientsByRevenue(salonId, from, to, PageRequest.of(0, 10))
        val clients = clientRepository.findAllById(clientProjections.map { it.clientId }).associateBy { it.id }
        val topClients = clientProjections.map { p ->
            val client = clients[p.clientId]
            ClientRevenueDto(
                clientId = p.clientId,
                clientName = client?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown",
                visits = p.visits,
                totalSpent = p.totalSpent ?: BigDecimal.ZERO,
            )
        }

        val dailyProjections = appointmentRepository.findDailyRevenue(salonId, from, to)
        val dailyBreakdown = dailyProjections.associate { p ->
            p.date.toString() to DailyStatsDto(
                date = p.date,
                appointments = p.appointmentCount,
                revenue = p.revenue ?: BigDecimal.ZERO,
            )
        }

        return BillingReportDetailsDto(
            report = report.toDto(),
            perEmployeeStats = perEmployeeStats,
            perServiceStats = perServiceStats,
            topClients = topClients,
            dailyBreakdown = dailyBreakdown,
        )
    }

    @Transactional
    fun generateInvoice(reportId: UUID, salonId: UUID): InvoiceDto {
        val report = reportRepository.findByIdAndSalonId(reportId, salonId)
            ?: throw com.beautyfinder.b2b.domain.billing.BillingDomainException("Report not found: $reportId")

        if (report.reportStatus == ReportStatus.INVOICED) {
            throw com.beautyfinder.b2b.domain.billing.BillingDomainException("Report already invoiced: $reportId")
        }

        val subscription = subscriptionRepository.findBySalonIdAndStatus(salonId, SubscriptionStatus.ACTIVE)
            ?: throw NoActiveSubscriptionException(salonId)

        val now = OffsetDateTime.now()
        val sequence = invoiceRepository.countInvoicesInMonth(salonId, report.year, report.month) + 1
        val invoiceNumber = InvoiceNumber.generate(salonId, report.year, report.month, sequence)

        val hasCommission = report.totalCommission > BigDecimal.ZERO
        val invoiceType = when {
            hasCommission -> InvoiceType.COMBINED
            else -> InvoiceType.SUBSCRIPTION
        }

        val netAmount = report.subscriptionFee.add(report.totalCommission).setScale(2, RoundingMode.HALF_UP)
        val vatRate = BigDecimal("0.23")
        val vatAmount = netAmount.multiply(vatRate).setScale(2, RoundingMode.HALF_UP)
        val grossAmount = netAmount.add(vatAmount).setScale(2, RoundingMode.HALF_UP)

        val invoice = invoiceRepository.save(
            Invoice(
                salonId = salonId,
                invoiceNumber = invoiceNumber.value,
                type = invoiceType,
                periodStart = report.periodStart,
                periodEnd = report.periodEnd,
                issuedAt = now,
                dueAt = now.plusDays(14),
                status = InvoiceStatus.ISSUED,
                netAmount = netAmount,
                vatRate = vatRate,
                vatAmount = vatAmount,
                grossAmount = grossAmount,
            )
        )

        val lineItems = mutableListOf<InvoiceLineItem>()

        lineItems.add(
            lineItemRepository.save(
                InvoiceLineItem(
                    invoiceId = invoice.id!!,
                    description = "Opłata abonamentowa - plan ${subscription.plan}",
                    quantity = BigDecimal.ONE,
                    unitPrice = report.subscriptionFee,
                    netAmount = report.subscriptionFee,
                    itemType = LineItemType.SUBSCRIPTION_FEE,
                )
            )
        )

        if (hasCommission) {
            lineItems.add(
                lineItemRepository.save(
                    InvoiceLineItem(
                        invoiceId = invoice.id!!,
                        description = "Prowizja od wizyt marketplace",
                        quantity = BigDecimal.ONE,
                        unitPrice = report.totalCommission,
                        netAmount = report.totalCommission,
                        itemType = LineItemType.COMMISSION,
                    )
                )
            )
        }

        report.reportStatus = ReportStatus.INVOICED
        report.invoiceId = invoice.id
        reportRepository.save(report)

        return invoice.toDto(lineItems)
    }

    @Transactional(readOnly = true)
    fun listInvoices(salonId: UUID, pageable: Pageable): Page<InvoiceSummaryDto> =
        invoiceRepository.findAllBySalonIdOrderByIssuedAtDesc(salonId, pageable)
            .map { it.toSummaryDto() }

    @Transactional(readOnly = true)
    fun getInvoice(invoiceId: UUID, salonId: UUID): InvoiceDto {
        val invoice = invoiceRepository.findByIdAndSalonId(invoiceId, salonId)
            ?: throw InvoiceNotFoundException(invoiceId)
        val lineItems = lineItemRepository.findAllByInvoiceId(invoiceId)
        return invoice.toDto(lineItems)
    }

    @Transactional
    fun markInvoiceAsPaid(invoiceId: UUID, salonId: UUID) {
        val invoice = invoiceRepository.findByIdAndSalonId(invoiceId, salonId)
            ?: throw InvoiceNotFoundException(invoiceId)

        if (invoice.status == InvoiceStatus.PAID) {
            throw InvoiceAlreadyPaidException(invoiceId)
        }

        invoice.status = InvoiceStatus.PAID
        invoice.paidAt = OffsetDateTime.now()
        invoiceRepository.save(invoice)
    }

    @Transactional(readOnly = true)
    fun getRevenueStats(salonId: UUID, period: StatsPeriod): RevenueStatsDto {
        val now = LocalDate.now()
        val (fromDate, toDate) = resolvePeriodRange(period, now)
        val from = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC)
        val to = toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

        val appointments = appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, from, to)
        val completed = appointments.filter { it.status == AppointmentStatus.COMPLETED }

        val totalRevenue = completed.sumOf { it.finalPrice ?: BigDecimal.ZERO }.setScale(2, RoundingMode.HALF_UP)
        val totalCommission = completed.sumOf { it.commissionValue ?: BigDecimal.ZERO }.setScale(2, RoundingMode.HALF_UP)

        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1
        val avgDailyRevenue = if (totalDays > 0) {
            totalRevenue.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO.setScale(2)

        // Revenue by month from existing reports
        val fromYearMonth = YearMonth.from(fromDate)
        val projections = reportRepository.findMonthlyRevenue(salonId, fromYearMonth.year, fromYearMonth.monthValue)
        val revenueByMonth = projections.map { p ->
            MonthlyRevenueDto(
                year = p.year,
                month = p.month,
                revenue = p.totalRevenue,
                commission = p.totalCommission,
                appointments = p.completedAppointments,
            )
        }

        // Revenue growth vs previous period
        val periodDays = totalDays
        val prevFrom = fromDate.minusDays(periodDays)
        val prevTo = fromDate.minusDays(1)
        val prevFromOdt = prevFrom.atStartOfDay().atOffset(ZoneOffset.UTC)
        val prevToOdt = prevTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

        val prevAppointments = appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, prevFromOdt, prevToOdt)
        val prevRevenue = prevAppointments
            .filter { it.status == AppointmentStatus.COMPLETED }
            .sumOf { it.finalPrice ?: BigDecimal.ZERO }

        val revenueGrowth = if (prevRevenue > BigDecimal.ZERO) {
            totalRevenue.subtract(prevRevenue)
                .multiply(BigDecimal(100))
                .divide(prevRevenue, 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO.setScale(2)

        // Projected month revenue
        val currentMonth = YearMonth.from(now)
        val dayOfMonth = now.dayOfMonth
        val daysInMonth = currentMonth.lengthOfMonth()
        val currentMonthFrom = currentMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        val currentMonthTo = now.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

        val currentMonthAppointments = appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(
            salonId, currentMonthFrom, currentMonthTo
        )
        val currentMonthRevenue = currentMonthAppointments
            .filter { it.status == AppointmentStatus.COMPLETED }
            .sumOf { it.finalPrice ?: BigDecimal.ZERO }

        val projectedMonthRevenue = if (dayOfMonth > 0) {
            currentMonthRevenue
                .multiply(BigDecimal.valueOf(daysInMonth.toLong()))
                .divide(BigDecimal.valueOf(dayOfMonth.toLong()), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO.setScale(2)

        return RevenueStatsDto(
            totalRevenue = totalRevenue,
            totalCommission = totalCommission,
            avgDailyRevenue = avgDailyRevenue,
            revenueByMonth = revenueByMonth,
            revenueGrowth = revenueGrowth,
            projectedMonthRevenue = projectedMonthRevenue,
        )
    }

    @Transactional(readOnly = true)
    fun getActiveSubscription(salonId: UUID): SalonSubscriptionDto {
        val subscription = subscriptionRepository.findBySalonIdAndStatus(salonId, SubscriptionStatus.ACTIVE)
            ?: throw NoActiveSubscriptionException(salonId)
        return subscription.toDto()
    }

    // -- Mappers --

    private fun BillingPeriodReport.toDto() = BillingPeriodReportDto(
        id = id!!,
        salonId = salonId,
        year = year,
        month = month,
        periodStart = periodStart,
        periodEnd = periodEnd,
        totalAppointments = totalAppointments,
        completedAppointments = completedAppointments,
        cancelledAppointments = cancelledAppointments,
        noShowAppointments = noShowAppointments,
        marketplaceAppointments = marketplaceAppointments,
        directAppointments = directAppointments,
        totalRevenue = totalRevenue,
        marketplaceRevenue = marketplaceRevenue,
        totalCommission = totalCommission,
        subscriptionFee = subscriptionFee,
        totalDue = totalCommission.add(subscriptionFee).setScale(2, RoundingMode.HALF_UP),
        reportStatus = reportStatus,
        generatedAt = generatedAt,
        invoiceId = invoiceId,
    )

    private fun BillingPeriodReport.toSummaryDto() = BillingPeriodReportSummaryDto(
        id = id!!,
        year = year,
        month = month,
        totalRevenue = totalRevenue,
        totalCommission = totalCommission,
        subscriptionFee = subscriptionFee,
        totalDue = totalCommission.add(subscriptionFee).setScale(2, RoundingMode.HALF_UP),
        reportStatus = reportStatus,
        invoiceId = invoiceId,
    )

    private fun Invoice.toDto(lineItems: List<InvoiceLineItem>) = InvoiceDto(
        id = id!!,
        invoiceNumber = invoiceNumber,
        type = type,
        periodStart = periodStart,
        periodEnd = periodEnd,
        issuedAt = issuedAt,
        dueAt = dueAt,
        status = status,
        netAmount = netAmount,
        vatRate = vatRate,
        vatAmount = vatAmount,
        grossAmount = grossAmount,
        currency = currency,
        paidAt = paidAt,
        notes = notes,
        lineItems = lineItems.map { it.toDto() },
    )

    private fun Invoice.toSummaryDto() = InvoiceSummaryDto(
        id = id!!,
        invoiceNumber = invoiceNumber,
        type = type,
        issuedAt = issuedAt,
        dueAt = dueAt,
        status = status,
        grossAmount = grossAmount,
        currency = currency,
        paidAt = paidAt,
    )

    private fun InvoiceLineItem.toDto() = InvoiceLineItemDto(
        id = id!!,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        netAmount = netAmount,
        itemType = itemType,
    )

    private fun com.beautyfinder.b2b.domain.billing.SalonSubscription.toDto() = SalonSubscriptionDto(
        id = id!!,
        salonId = salonId,
        plan = plan,
        monthlyFee = monthlyFee,
        commissionRate = commissionRate,
        validFrom = validFrom,
        validTo = validTo,
        status = status,
    )

    private fun resolvePeriodRange(period: StatsPeriod, now: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            StatsPeriod.LAST_7_DAYS -> now.minusDays(6) to now
            StatsPeriod.LAST_30_DAYS -> now.minusDays(29) to now
            StatsPeriod.LAST_3_MONTHS -> now.minusMonths(3).withDayOfMonth(1) to now
            StatsPeriod.LAST_12_MONTHS -> now.minusMonths(12).withDayOfMonth(1) to now
            StatsPeriod.CURRENT_MONTH -> YearMonth.from(now).atDay(1) to now
            StatsPeriod.CURRENT_YEAR -> now.withDayOfYear(1) to now
        }
}

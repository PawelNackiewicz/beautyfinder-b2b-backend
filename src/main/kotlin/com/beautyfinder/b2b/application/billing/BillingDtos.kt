package com.beautyfinder.b2b.application.billing

import com.beautyfinder.b2b.domain.billing.InvoiceStatus
import com.beautyfinder.b2b.domain.billing.InvoiceType
import com.beautyfinder.b2b.domain.billing.LineItemType
import com.beautyfinder.b2b.domain.billing.ReportStatus
import com.beautyfinder.b2b.domain.billing.SubscriptionPlan
import com.beautyfinder.b2b.domain.billing.SubscriptionStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// -- Report DTOs --

data class BillingPeriodReportDto(
    val id: UUID,
    val salonId: UUID,
    val year: Int,
    val month: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalAppointments: Int,
    val completedAppointments: Int,
    val cancelledAppointments: Int,
    val noShowAppointments: Int,
    val marketplaceAppointments: Int,
    val directAppointments: Int,
    val totalRevenue: BigDecimal,
    val marketplaceRevenue: BigDecimal,
    val totalCommission: BigDecimal,
    val subscriptionFee: BigDecimal,
    val totalDue: BigDecimal,
    val reportStatus: ReportStatus,
    val generatedAt: OffsetDateTime?,
    val invoiceId: UUID?,
)

data class BillingPeriodReportSummaryDto(
    val id: UUID,
    val year: Int,
    val month: Int,
    val totalRevenue: BigDecimal,
    val totalCommission: BigDecimal,
    val subscriptionFee: BigDecimal,
    val totalDue: BigDecimal,
    val reportStatus: ReportStatus,
    val invoiceId: UUID?,
)

data class BillingReportDetailsDto(
    val report: BillingPeriodReportDto,
    val perEmployeeStats: List<EmployeeRevenueDto>,
    val perServiceStats: List<ServiceRevenueDto>,
    val topClients: List<ClientRevenueDto>,
    val dailyBreakdown: Map<String, DailyStatsDto>,
)

data class EmployeeRevenueDto(
    val employeeId: UUID,
    val employeeName: String,
    val completedAppointments: Long,
    val revenue: BigDecimal,
    val commission: BigDecimal,
)

data class ServiceRevenueDto(
    val serviceId: UUID,
    val serviceName: String,
    val completedAppointments: Long,
    val revenue: BigDecimal,
)

data class ClientRevenueDto(
    val clientId: UUID,
    val clientName: String,
    val visits: Long,
    val totalSpent: BigDecimal,
)

data class DailyStatsDto(
    val date: LocalDate,
    val appointments: Long,
    val revenue: BigDecimal,
)

// -- Invoice DTOs --

data class InvoiceDto(
    val id: UUID,
    val invoiceNumber: String,
    val type: InvoiceType,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val issuedAt: OffsetDateTime,
    val dueAt: OffsetDateTime,
    val status: InvoiceStatus,
    val netAmount: BigDecimal,
    val vatRate: BigDecimal,
    val vatAmount: BigDecimal,
    val grossAmount: BigDecimal,
    val currency: String,
    val paidAt: OffsetDateTime?,
    val notes: String?,
    val lineItems: List<InvoiceLineItemDto>,
)

data class InvoiceLineItemDto(
    val id: UUID,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val netAmount: BigDecimal,
    val itemType: LineItemType,
)

data class InvoiceSummaryDto(
    val id: UUID,
    val invoiceNumber: String,
    val type: InvoiceType,
    val issuedAt: OffsetDateTime,
    val dueAt: OffsetDateTime,
    val status: InvoiceStatus,
    val grossAmount: BigDecimal,
    val currency: String,
    val paidAt: OffsetDateTime?,
)

// -- Revenue Stats DTOs --

enum class StatsPeriod {
    LAST_7_DAYS, LAST_30_DAYS, LAST_3_MONTHS, LAST_12_MONTHS, CURRENT_MONTH, CURRENT_YEAR
}

data class RevenueStatsDto(
    val totalRevenue: BigDecimal,
    val totalCommission: BigDecimal,
    val avgDailyRevenue: BigDecimal,
    val revenueByMonth: List<MonthlyRevenueDto>,
    val revenueGrowth: BigDecimal,
    val projectedMonthRevenue: BigDecimal,
)

data class MonthlyRevenueDto(
    val year: Int,
    val month: Int,
    val revenue: BigDecimal,
    val commission: BigDecimal,
    val appointments: Int,
)

// -- Subscription DTOs --

data class SalonSubscriptionDto(
    val id: UUID,
    val salonId: UUID,
    val plan: SubscriptionPlan,
    val monthlyFee: BigDecimal,
    val commissionRate: BigDecimal,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: SubscriptionStatus,
)

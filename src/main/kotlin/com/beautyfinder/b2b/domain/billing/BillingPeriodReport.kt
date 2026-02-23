package com.beautyfinder.b2b.domain.billing

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class ReportStatus {
    PENDING, GENERATED, INVOICED
}

@Entity
@Table(
    name = "billing_period_reports",
    uniqueConstraints = [UniqueConstraint(columnNames = ["salon_id", "year", "month"])]
)
class BillingPeriodReport(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(nullable = false)
    val year: Int,

    @Column(nullable = false)
    val month: Int,

    @Column(name = "period_start", nullable = false)
    val periodStart: LocalDate,

    @Column(name = "period_end", nullable = false)
    val periodEnd: LocalDate,

    @Column(name = "total_appointments", nullable = false)
    val totalAppointments: Int,

    @Column(name = "completed_appointments", nullable = false)
    val completedAppointments: Int,

    @Column(name = "cancelled_appointments", nullable = false)
    val cancelledAppointments: Int,

    @Column(name = "no_show_appointments", nullable = false)
    val noShowAppointments: Int,

    @Column(name = "marketplace_appointments", nullable = false)
    val marketplaceAppointments: Int,

    @Column(name = "direct_appointments", nullable = false)
    val directAppointments: Int,

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    val totalRevenue: BigDecimal,

    @Column(name = "marketplace_revenue", nullable = false, precision = 12, scale = 2)
    val marketplaceRevenue: BigDecimal,

    @Column(name = "total_commission", nullable = false, precision = 12, scale = 2)
    val totalCommission: BigDecimal,

    @Column(name = "subscription_fee", nullable = false, precision = 10, scale = 2)
    val subscriptionFee: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false)
    var reportStatus: ReportStatus = ReportStatus.GENERATED,

    @Column(name = "generated_at")
    var generatedAt: OffsetDateTime? = null,

    @Column(name = "invoice_id")
    var invoiceId: UUID? = null,
) : BaseEntity()

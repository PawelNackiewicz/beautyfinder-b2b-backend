package com.beautyfinder.b2b.domain.billing

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class InvoiceType {
    SUBSCRIPTION, COMMISSION, COMBINED
}

enum class InvoiceStatus {
    DRAFT, ISSUED, PAID, OVERDUE, CANCELLED
}

@Entity
@Table(name = "invoices")
class Invoice(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(name = "invoice_number", nullable = false, unique = true)
    val invoiceNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: InvoiceType,

    @Column(name = "period_start", nullable = false)
    val periodStart: LocalDate,

    @Column(name = "period_end", nullable = false)
    val periodEnd: LocalDate,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: OffsetDateTime,

    @Column(name = "due_at", nullable = false)
    val dueAt: OffsetDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InvoiceStatus = InvoiceStatus.ISSUED,

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    val netAmount: BigDecimal,

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    val vatRate: BigDecimal,

    @Column(name = "vat_amount", nullable = false, precision = 10, scale = 2)
    val vatAmount: BigDecimal,

    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    val grossAmount: BigDecimal,

    @Column(nullable = false, length = 3)
    val currency: String = "PLN",

    @Column(name = "paid_at")
    var paidAt: OffsetDateTime? = null,

    @Column(length = 1000)
    var notes: String? = null,
) : BaseEntity()

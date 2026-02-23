package com.beautyfinder.b2b.domain.billing

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

enum class LineItemType {
    SUBSCRIPTION_FEE, COMMISSION, DISCOUNT, ADJUSTMENT
}

@Entity
@Table(name = "invoice_line_items")
class InvoiceLineItem(
    @Column(name = "invoice_id", nullable = false)
    val invoiceId: UUID,

    @Column(nullable = false, length = 500)
    val description: String,

    @Column(nullable = false, precision = 10, scale = 2)
    val quantity: BigDecimal,

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    val unitPrice: BigDecimal,

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    val netAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    val itemType: LineItemType,

    @Column(name = "appointment_id")
    val appointmentId: UUID? = null,
) : BaseEntity()

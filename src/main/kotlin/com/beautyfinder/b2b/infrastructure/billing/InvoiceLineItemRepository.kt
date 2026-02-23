package com.beautyfinder.b2b.infrastructure.billing

import com.beautyfinder.b2b.domain.billing.InvoiceLineItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InvoiceLineItemRepository : JpaRepository<InvoiceLineItem, UUID> {

    fun findAllByInvoiceId(invoiceId: UUID): List<InvoiceLineItem>
}

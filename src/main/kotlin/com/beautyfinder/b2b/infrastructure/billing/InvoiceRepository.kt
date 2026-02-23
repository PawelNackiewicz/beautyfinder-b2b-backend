package com.beautyfinder.b2b.infrastructure.billing

import com.beautyfinder.b2b.domain.billing.Invoice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface InvoiceRepository : JpaRepository<Invoice, UUID> {

    fun findByIdAndSalonId(id: UUID, salonId: UUID): Invoice?

    fun findAllBySalonIdOrderByIssuedAtDesc(salonId: UUID, pageable: Pageable): Page<Invoice>

    @Query(
        """
        SELECT COUNT(i) FROM Invoice i
        WHERE i.salonId = :salonId
        AND YEAR(i.issuedAt) = :year
        AND MONTH(i.issuedAt) = :month
        """
    )
    fun countInvoicesInMonth(
        @Param("salonId") salonId: UUID,
        @Param("year") year: Int,
        @Param("month") month: Int,
    ): Int
}

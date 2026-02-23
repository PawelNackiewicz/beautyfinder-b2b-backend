package com.beautyfinder.b2b.infrastructure.billing

import com.beautyfinder.b2b.domain.billing.BillingPeriodReport
import com.beautyfinder.b2b.domain.billing.ReportStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface BillingPeriodReportRepository : JpaRepository<BillingPeriodReport, UUID> {

    fun findBySalonIdAndYearAndMonth(salonId: UUID, year: Int, month: Int): BillingPeriodReport?

    fun findAllBySalonIdOrderByYearDescMonthDesc(salonId: UUID, pageable: Pageable): Page<BillingPeriodReport>

    fun findAllByReportStatus(status: ReportStatus): List<BillingPeriodReport>

    fun findByIdAndSalonId(id: UUID, salonId: UUID): BillingPeriodReport?

    @Query(
        """
        SELECT r.year as year, r.month as month, r.totalRevenue as totalRevenue,
               r.totalCommission as totalCommission, r.completedAppointments as completedAppointments
        FROM BillingPeriodReport r
        WHERE r.salonId = :salonId
        AND (r.year > :fromYear OR (r.year = :fromYear AND r.month >= :fromMonth))
        ORDER BY r.year DESC, r.month DESC
        """
    )
    fun findMonthlyRevenue(
        @Param("salonId") salonId: UUID,
        @Param("fromYear") fromYear: Int,
        @Param("fromMonth") fromMonth: Int,
    ): List<MonthlyRevenueProjection>
}

interface MonthlyRevenueProjection {
    val year: Int
    val month: Int
    val totalRevenue: java.math.BigDecimal
    val totalCommission: java.math.BigDecimal
    val completedAppointments: Int
}

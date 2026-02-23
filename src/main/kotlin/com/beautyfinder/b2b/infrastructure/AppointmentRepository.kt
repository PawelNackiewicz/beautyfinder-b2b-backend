package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.service.ServiceVariant
import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface AppointmentRepository : JpaRepository<Appointment, UUID> {

    fun findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(
        salonId: UUID,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<Appointment>

    fun findAllBySalonIdAndEmployeeIdAndStartAtBetweenOrderByStartAtAsc(
        salonId: UUID,
        employeeId: UUID,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<Appointment>

    fun findAllBySalonId(salonId: UUID, pageable: Pageable): Page<Appointment>

    fun findAllBySalonId(salonId: UUID): List<Appointment>

    fun findByIdAndSalonId(id: UUID, salonId: UUID): Appointment?

    @Query(
        """
        SELECT a FROM Appointment a WHERE a.employeeId = :employeeId
        AND a.status IN :statuses
        AND a.startAt < :endAt
        AND a.endAt > :startAt
        AND a.id != :excludeId
        """
    )
    fun findConflictingAppointments(
        @Param("employeeId") employeeId: UUID,
        @Param("startAt") startAt: OffsetDateTime,
        @Param("endAt") endAt: OffsetDateTime,
        @Param("statuses") statuses: List<AppointmentStatus>,
        @Param("excludeId") excludeId: UUID,
    ): List<Appointment>

    @Query(
        """
        SELECT a FROM Appointment a WHERE a.employeeId = :employeeId
        AND a.status IN :statuses
        AND a.startAt < :endAt
        AND a.endAt > :startAt
        """
    )
    fun findConflictingAppointments(
        @Param("employeeId") employeeId: UUID,
        @Param("startAt") startAt: OffsetDateTime,
        @Param("endAt") endAt: OffsetDateTime,
        @Param("statuses") statuses: List<AppointmentStatus>,
    ): List<Appointment>

    fun findAllByStatusInAndEndAtBefore(
        statuses: List<AppointmentStatus>,
        endAt: OffsetDateTime,
    ): List<Appointment>

    // -- Billing revenue projections --

    @Query(
        """
        SELECT a.employeeId as employeeId,
               SUM(a.finalPrice) as revenue,
               SUM(a.commissionValue) as commission,
               COUNT(a) as appointmentCount
        FROM Appointment a
        WHERE a.salonId = :salonId
        AND a.status = 'COMPLETED'
        AND a.startAt >= :from AND a.startAt < :to
        GROUP BY a.employeeId
        """
    )
    fun findRevenueByEmployee(
        @Param("salonId") salonId: UUID,
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
    ): List<EmployeeRevenueProjection>

    @Query(
        """
        SELECT sv.serviceId as serviceId,
               COUNT(a) as appointmentCount,
               SUM(a.finalPrice) as revenue
        FROM Appointment a JOIN ServiceVariant sv ON a.variantId = sv.id
        WHERE a.salonId = :salonId
        AND a.status = 'COMPLETED'
        AND a.startAt >= :from AND a.startAt < :to
        GROUP BY sv.serviceId
        """
    )
    fun findRevenueByService(
        @Param("salonId") salonId: UUID,
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
    ): List<ServiceRevenueProjection>

    @Query(
        """
        SELECT CAST(a.startAt AS LocalDate) as date,
               COUNT(a) as appointmentCount,
               SUM(a.finalPrice) as revenue
        FROM Appointment a
        WHERE a.salonId = :salonId
        AND a.status = 'COMPLETED'
        AND a.startAt >= :from AND a.startAt < :to
        GROUP BY CAST(a.startAt AS LocalDate)
        ORDER BY CAST(a.startAt AS LocalDate)
        """
    )
    fun findDailyRevenue(
        @Param("salonId") salonId: UUID,
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
    ): List<DailyRevenueProjection>

    @Query(
        """
        SELECT a.clientId as clientId,
               COUNT(a) as visits,
               SUM(a.finalPrice) as totalSpent
        FROM Appointment a
        WHERE a.salonId = :salonId
        AND a.status = 'COMPLETED'
        AND a.startAt >= :from AND a.startAt < :to
        GROUP BY a.clientId
        ORDER BY SUM(a.finalPrice) DESC
        """
    )
    fun findTopClientsByRevenue(
        @Param("salonId") salonId: UUID,
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
        pageable: Pageable,
    ): List<ClientRevenueProjection>
}

interface EmployeeRevenueProjection {
    val employeeId: UUID
    val revenue: java.math.BigDecimal?
    val commission: java.math.BigDecimal?
    val appointmentCount: Long
}

interface ServiceRevenueProjection {
    val serviceId: UUID
    val appointmentCount: Long
    val revenue: java.math.BigDecimal?
}

interface DailyRevenueProjection {
    val date: java.time.LocalDate
    val appointmentCount: Long
    val revenue: java.math.BigDecimal?
}

interface ClientRevenueProjection {
    val clientId: UUID
    val visits: Long
    val totalSpent: java.math.BigDecimal?
}

package com.beautyfinder.b2b.infrastructure

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
}

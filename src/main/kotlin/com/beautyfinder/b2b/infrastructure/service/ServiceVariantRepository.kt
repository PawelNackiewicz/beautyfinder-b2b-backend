package com.beautyfinder.b2b.infrastructure.service

import com.beautyfinder.b2b.domain.service.ServiceVariant
import com.beautyfinder.b2b.domain.service.VariantStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface ServiceVariantRepository : JpaRepository<ServiceVariant, UUID> {

    fun findByIdAndSalonId(id: UUID, salonId: UUID): ServiceVariant?

    fun findAllByServiceIdAndStatusOrderByDisplayOrderAsc(serviceId: UUID, status: VariantStatus): List<ServiceVariant>

    fun findAllByServiceIdOrderByDisplayOrderAsc(serviceId: UUID): List<ServiceVariant>

    fun findAllBySalonId(salonId: UUID): List<ServiceVariant>

    fun existsByIdAndSalonId(id: UUID, salonId: UUID): Boolean

    @Query("SELECT MAX(v.displayOrder) FROM ServiceVariant v WHERE v.serviceId = :serviceId")
    fun findMaxDisplayOrder(@Param("serviceId") serviceId: UUID): Int?

    @Query(
        """
        SELECT COUNT(a) FROM Appointment a WHERE a.variantId = :variantId
        AND a.status IN :statuses AND a.startAt > :now
        """,
    )
    fun countFutureAppointments(
        @Param("variantId") variantId: UUID,
        @Param("statuses") statuses: List<com.beautyfinder.b2b.domain.appointment.AppointmentStatus>,
        @Param("now") now: OffsetDateTime,
    ): Long

    @Modifying
    @Query("UPDATE ServiceVariant v SET v.displayOrder = :order WHERE v.id = :id")
    fun updateDisplayOrder(@Param("id") id: UUID, @Param("order") order: Int)
}

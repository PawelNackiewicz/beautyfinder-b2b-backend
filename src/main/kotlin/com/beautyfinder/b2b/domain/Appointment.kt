package com.beautyfinder.b2b.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class AppointmentStatus {
    SCHEDULED, COMPLETED, NO_SHOW, CANCELLED
}

@Entity
@Table(name = "appointments")
class Appointment(
    @Column(name = "salon_id", nullable = false)
    var salonId: UUID,

    @Column(name = "client_id", nullable = false)
    var clientId: UUID,

    @Column(name = "employee_id", nullable = false)
    var employeeId: UUID,

    @Column(name = "variant_id", nullable = false)
    var variantId: UUID,

    @Column(name = "start_at", nullable = false)
    var startAt: OffsetDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AppointmentStatus = AppointmentStatus.SCHEDULED,

    @Column(name = "final_price", precision = 10, scale = 2)
    var finalPrice: BigDecimal? = null,

    var source: String? = null,
) : BaseEntity()

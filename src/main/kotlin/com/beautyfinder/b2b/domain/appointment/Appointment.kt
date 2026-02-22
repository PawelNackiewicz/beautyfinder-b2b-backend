package com.beautyfinder.b2b.domain.appointment

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class AppointmentStatus {
    SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, NO_SHOW, CANCELLED
}

enum class AppointmentSource {
    DIRECT, MARKETPLACE
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

    @Column(name = "end_at", nullable = false)
    var endAt: OffsetDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AppointmentStatus = AppointmentStatus.SCHEDULED,

    @Column(name = "final_price", precision = 10, scale = 2)
    var finalPrice: BigDecimal? = null,

    @Column(name = "commission_value", precision = 10, scale = 2)
    var commissionValue: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var source: AppointmentSource = AppointmentSource.DIRECT,

    @Column(length = 500)
    var notes: String? = null,

    @Column(name = "cancellation_reason", length = 500)
    var cancellationReason: String? = null,
) : BaseEntity()

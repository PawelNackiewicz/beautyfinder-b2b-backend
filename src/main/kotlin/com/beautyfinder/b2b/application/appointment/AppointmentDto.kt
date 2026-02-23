package com.beautyfinder.b2b.application.appointment

import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class AppointmentDto(
    val id: UUID,
    val salonId: UUID,
    val clientId: UUID,
    val clientName: String,
    val employeeId: UUID,
    val employeeName: String,
    val variantId: UUID,
    val variantName: String,
    val serviceName: String,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val status: AppointmentStatus,
    val source: AppointmentSource,
    val finalPrice: BigDecimal?,
    val commissionValue: BigDecimal?,
    val notes: String?,
    val cancellationReason: String?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

data class AppointmentQuery(
    val date: java.time.LocalDate,
    val employeeId: UUID? = null,
    val includeStatuses: List<AppointmentStatus>? = null,
)

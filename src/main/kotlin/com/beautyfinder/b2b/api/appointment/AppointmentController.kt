package com.beautyfinder.b2b.api.appointment

import com.beautyfinder.b2b.application.appointment.AppointmentQuery
import com.beautyfinder.b2b.application.appointment.AppointmentService
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import com.beautyfinder.b2b.application.appointment.CreateAppointmentRequest as ServiceCreateRequest

// --- Request DTOs ---

data class CreateAppointmentApiRequest(
    @field:NotNull val clientId: UUID,
    @field:NotNull val employeeId: UUID,
    @field:NotNull val variantId: UUID,
    @field:NotNull @field:Future val startAt: OffsetDateTime,
    val source: AppointmentSource = AppointmentSource.DIRECT,
    @field:Size(max = 500) val notes: String? = null,
)

data class UpdateStatusRequest(
    @field:NotNull val newStatus: AppointmentStatus,
    @field:Size(max = 500) val reason: String? = null,
)

data class RescheduleRequest(
    @field:NotNull @field:Future val newStartAt: OffsetDateTime,
)

// --- Response DTO ---

data class AppointmentResponse(
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
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

// --- Controller ---

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Appointment management endpoints")
class AppointmentController(
    private val appointmentService: AppointmentService,
) {

    @GetMapping
    @Operation(summary = "Get daily appointments")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getAppointments(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam(required = false) employeeId: UUID?,
        @RequestParam(required = false) statuses: List<AppointmentStatus>?,
    ): List<AppointmentResponse> {
        val salonId = TenantContext.getSalonId()
        val query = AppointmentQuery(date = date, employeeId = employeeId, includeStatuses = statuses)
        return appointmentService.getAppointments(query, salonId).map { it.toResponse() }
    }

    @GetMapping("/week")
    @Operation(summary = "Get weekly calendar view")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getWeekAppointments(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) weekStart: LocalDate,
    ): Map<String, List<AppointmentResponse>> {
        val salonId = TenantContext.getSalonId()
        return appointmentService.getWeekAppointments(weekStart, salonId)
            .mapKeys { it.key.toString() }
            .mapValues { entry -> entry.value.map { it.toResponse() } }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment details")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getAppointmentById(@PathVariable id: UUID): AppointmentResponse {
        val salonId = TenantContext.getSalonId()
        return appointmentService.getAppointmentById(id, salonId).toResponse()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create appointment")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun createAppointment(@Valid @RequestBody request: CreateAppointmentApiRequest): AppointmentResponse {
        val salonId = TenantContext.getSalonId()
        val serviceRequest = ServiceCreateRequest(
            clientId = request.clientId,
            employeeId = request.employeeId,
            variantId = request.variantId,
            startAt = request.startAt,
            source = request.source,
            notes = request.notes,
        )
        return appointmentService.createAppointment(serviceRequest, salonId).toResponse()
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateStatusRequest,
    ): AppointmentResponse {
        val salonId = TenantContext.getSalonId()
        return appointmentService.updateAppointmentStatus(id, request.newStatus, request.reason, salonId).toResponse()
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule appointment")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun reschedule(
        @PathVariable id: UUID,
        @Valid @RequestBody request: RescheduleRequest,
    ): AppointmentResponse {
        val salonId = TenantContext.getSalonId()
        return appointmentService.rescheduleAppointment(id, request.newStartAt, salonId).toResponse()
    }
}

private fun com.beautyfinder.b2b.application.appointment.AppointmentDto.toResponse() = AppointmentResponse(
    id = id,
    salonId = salonId,
    clientId = clientId,
    clientName = clientName,
    employeeId = employeeId,
    employeeName = employeeName,
    variantId = variantId,
    variantName = variantName,
    serviceName = serviceName,
    startAt = startAt,
    endAt = endAt,
    status = status,
    source = source,
    finalPrice = finalPrice,
    commissionValue = commissionValue,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

package com.beautyfinder.b2b.api

import com.beautyfinder.b2b.application.AppointmentService
import com.beautyfinder.b2b.config.TenantContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class CreateAppointmentRequest(
    @field:NotNull val clientId: UUID,
    @field:NotNull val employeeId: UUID,
    @field:NotNull val variantId: UUID,
    @field:NotNull val startAt: OffsetDateTime,
)

data class AppointmentResponse(
    val id: UUID,
    val salonId: UUID,
    val clientId: UUID,
    val employeeId: UUID,
    val variantId: UUID,
    val startAt: OffsetDateTime,
    val status: String,
    val finalPrice: BigDecimal?,
    val source: String?,
    val createdAt: OffsetDateTime?,
)

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Appointment management endpoints")
class AppointmentController(
    private val appointmentService: AppointmentService,
) {

    @GetMapping
    @Operation(summary = "List appointments", description = "Returns all appointments for the current salon")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun listAppointments(): List<AppointmentResponse> {
        val salonId = TenantContext.getSalonId()
        return appointmentService.listAppointments(salonId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create appointment", description = "Creates a new appointment")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun createAppointment(@Valid @RequestBody request: CreateAppointmentRequest): AppointmentResponse {
        val salonId = TenantContext.getSalonId()
        return appointmentService.createAppointment(request, salonId)
    }
}

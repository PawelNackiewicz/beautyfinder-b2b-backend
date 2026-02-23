package com.beautyfinder.b2b.api.employee

import com.beautyfinder.b2b.application.employee.AvailableSlotDto
import com.beautyfinder.b2b.application.employee.EmployeeDto
import com.beautyfinder.b2b.application.employee.EmployeeService
import com.beautyfinder.b2b.application.employee.ScheduleExceptionDto
import com.beautyfinder.b2b.application.employee.WeeklyScheduleDto
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.employee.EmployeeStatus
import com.beautyfinder.b2b.domain.schedule.ScheduleExceptionType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

// --- API Request DTOs ---

data class CreateEmployeeApiRequest(
    @field:NotNull val userId: UUID,
    @field:NotBlank @field:Size(max = 100) val displayName: String,
    @field:Pattern(regexp = "\\+?[0-9]{9,15}") val phone: String? = null,
    @field:Size(max = 500) val avatarUrl: String? = null,
    @field:Pattern(regexp = "#[0-9A-Fa-f]{6}") val color: String? = null,
    val serviceIds: Set<UUID> = emptySet(),
    val weeklySchedule: List<DayScheduleApiRequest>? = null,
)

data class UpdateEmployeeApiRequest(
    @field:Size(max = 100) val displayName: String? = null,
    @field:Pattern(regexp = "\\+?[0-9]{9,15}") val phone: String? = null,
    @field:Size(max = 500) val avatarUrl: String? = null,
    @field:Pattern(regexp = "#[0-9A-Fa-f]{6}") val color: String? = null,
    val serviceIds: Set<UUID>? = null,
)

data class DayScheduleApiRequest(
    @field:NotNull val dayOfWeek: DayOfWeek,
    val isWorkingDay: Boolean = false,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
)

data class AddExceptionApiRequest(
    @field:NotNull @field:Future val startAt: OffsetDateTime,
    @field:NotNull val endAt: OffsetDateTime,
    @field:Size(max = 500) val reason: String? = null,
    @field:NotNull val type: ScheduleExceptionType,
)

// --- API Response DTOs ---

data class EmployeeResponse(
    val id: UUID,
    val salonId: UUID,
    val userId: UUID,
    val displayName: String,
    val phone: String?,
    val avatarUrl: String?,
    val color: String?,
    val status: EmployeeStatus,
    val serviceIds: Set<UUID>,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

data class WeeklyScheduleResponse(
    val id: UUID?,
    val employeeId: UUID,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isWorkingDay: Boolean,
)

data class ScheduleExceptionResponse(
    val id: UUID,
    val employeeId: UUID,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val reason: String?,
    val type: ScheduleExceptionType,
    val createdAt: OffsetDateTime?,
)

data class AvailableSlotResponse(
    val start: OffsetDateTime,
    val end: OffsetDateTime,
    val durationMinutes: Int,
)

// --- Controller ---

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee and schedule management")
class EmployeeController(
    private val employeeService: EmployeeService,
) {

    @GetMapping
    @Operation(summary = "List employees")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun getEmployees(
        @RequestParam(defaultValue = "false") includeInactive: Boolean,
    ): List<EmployeeResponse> {
        val salonId = TenantContext.getSalonId()
        return employeeService.listEmployees(salonId, includeInactive).map { it.toResponse() }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee details")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getEmployee(@PathVariable id: UUID): EmployeeResponse {
        val salonId = TenantContext.getSalonId()
        return employeeService.getEmployee(id, salonId).toResponse()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create employee")
    @PreAuthorize("hasRole('OWNER')")
    fun createEmployee(@Valid @RequestBody request: CreateEmployeeApiRequest): EmployeeResponse {
        val salonId = TenantContext.getSalonId()
        val serviceRequest = com.beautyfinder.b2b.application.employee.CreateEmployeeRequest(
            userId = request.userId,
            displayName = request.displayName,
            phone = request.phone,
            avatarUrl = request.avatarUrl,
            color = request.color,
            serviceIds = request.serviceIds,
            weeklySchedule = request.weeklySchedule?.map {
                com.beautyfinder.b2b.application.employee.UpsertScheduleRequest(
                    dayOfWeek = it.dayOfWeek,
                    isWorkingDay = it.isWorkingDay,
                    startTime = it.startTime,
                    endTime = it.endTime,
                )
            },
        )
        return employeeService.createEmployee(serviceRequest, salonId).toResponse()
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun updateEmployee(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateEmployeeApiRequest,
    ): EmployeeResponse {
        val salonId = TenantContext.getSalonId()
        val serviceRequest = com.beautyfinder.b2b.application.employee.UpdateEmployeeRequest(
            displayName = request.displayName,
            phone = request.phone,
            avatarUrl = request.avatarUrl,
            color = request.color,
            serviceIds = request.serviceIds,
        )
        return employeeService.updateEmployee(id, serviceRequest, salonId).toResponse()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate employee (soft delete)")
    @PreAuthorize("hasRole('OWNER')")
    fun deactivateEmployee(@PathVariable id: UUID) {
        val salonId = TenantContext.getSalonId()
        employeeService.deactivateEmployee(id, salonId)
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Get weekly schedule")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getSchedule(@PathVariable id: UUID): List<WeeklyScheduleResponse> {
        val salonId = TenantContext.getSalonId()
        return employeeService.getWeeklySchedule(id, salonId).map { it.toScheduleResponse() }
    }

    @PutMapping("/{id}/schedule")
    @Operation(summary = "Replace weekly schedule")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun upsertSchedule(
        @PathVariable id: UUID,
        @Valid @RequestBody schedules: List<DayScheduleApiRequest>,
    ): List<WeeklyScheduleResponse> {
        val salonId = TenantContext.getSalonId()
        val serviceRequests = schedules.map {
            com.beautyfinder.b2b.application.employee.UpsertScheduleRequest(
                dayOfWeek = it.dayOfWeek,
                isWorkingDay = it.isWorkingDay,
                startTime = it.startTime,
                endTime = it.endTime,
            )
        }
        return employeeService.upsertWeeklySchedule(id, serviceRequests, salonId).map { it.toScheduleResponse() }
    }

    @GetMapping("/{id}/exceptions")
    @Operation(summary = "List schedule exceptions")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getExceptions(
        @PathVariable id: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<ScheduleExceptionResponse> {
        val salonId = TenantContext.getSalonId()
        return employeeService.listScheduleExceptions(id, salonId, from, to).map { it.toExceptionResponse() }
    }

    @PostMapping("/{id}/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add schedule exception")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun addException(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddExceptionApiRequest,
    ): ScheduleExceptionResponse {
        val salonId = TenantContext.getSalonId()
        val serviceRequest = com.beautyfinder.b2b.application.employee.AddExceptionRequest(
            startAt = request.startAt,
            endAt = request.endAt,
            reason = request.reason,
            type = request.type,
        )
        return employeeService.addScheduleException(id, serviceRequest, salonId).toExceptionResponse()
    }

    @DeleteMapping("/{id}/exceptions/{exceptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete schedule exception")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun deleteException(@PathVariable id: UUID, @PathVariable exceptionId: UUID) {
        val salonId = TenantContext.getSalonId()
        employeeService.deleteScheduleException(exceptionId, id, salonId)
    }

    @GetMapping("/{id}/available-slots")
    @Operation(summary = "Get available time slots")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getAvailableSlots(
        @PathVariable id: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam @Min(15) @Max(480) variantDurationMinutes: Int,
    ): List<AvailableSlotResponse> {
        val salonId = TenantContext.getSalonId()
        return employeeService.getAvailableSlots(id, date, variantDurationMinutes, salonId).map { it.toSlotResponse() }
    }
}

// --- Extension mappers ---

private fun EmployeeDto.toResponse() = EmployeeResponse(
    id = id, salonId = salonId, userId = userId, displayName = displayName,
    phone = phone, avatarUrl = avatarUrl, color = color, status = status,
    serviceIds = serviceIds, createdAt = createdAt, updatedAt = updatedAt,
)

private fun WeeklyScheduleDto.toScheduleResponse() = WeeklyScheduleResponse(
    id = id, employeeId = employeeId, dayOfWeek = dayOfWeek,
    startTime = startTime, endTime = endTime, isWorkingDay = isWorkingDay,
)

private fun ScheduleExceptionDto.toExceptionResponse() = ScheduleExceptionResponse(
    id = id, employeeId = employeeId, startAt = startAt, endAt = endAt,
    reason = reason, type = type, createdAt = createdAt,
)

private fun AvailableSlotDto.toSlotResponse() = AvailableSlotResponse(
    start = start, end = end, durationMinutes = durationMinutes,
)

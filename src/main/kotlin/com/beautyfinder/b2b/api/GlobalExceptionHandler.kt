package com.beautyfinder.b2b.api

import com.beautyfinder.b2b.domain.appointment.AppointmentConflictException
import com.beautyfinder.b2b.domain.appointment.AppointmentNotFoundException
import com.beautyfinder.b2b.domain.appointment.CancellationWindowExpiredException
import com.beautyfinder.b2b.domain.appointment.EmployeeNotAvailableException
import com.beautyfinder.b2b.domain.appointment.InvalidStatusTransitionException
import com.beautyfinder.b2b.domain.employee.CannotDeleteActiveEmployeeException
import com.beautyfinder.b2b.domain.employee.EmployeeDomainException
import com.beautyfinder.b2b.domain.employee.EmployeeNotFoundException
import com.beautyfinder.b2b.domain.employee.InvalidScheduleException
import com.beautyfinder.b2b.domain.employee.ScheduleExceptionOverlapException
import com.beautyfinder.b2b.domain.employee.ScheduleOverlapException
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

data class ErrorResponse(
    val timestamp: OffsetDateTime,
    val status: Int,
    val message: String,
    val path: String,
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleEntityNotFound(ex: EntityNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Entity not found",
            path = request.requestURI,
        )

    @ExceptionHandler(AppointmentNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleAppointmentNotFound(ex: AppointmentNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Appointment not found",
            path = request.requestURI,
        )

    @ExceptionHandler(EmployeeNotAvailableException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleEmployeeNotAvailable(ex: EmployeeNotAvailableException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = "Employee not available in requested time slot",
            path = request.requestURI,
        )

    @ExceptionHandler(AppointmentConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleAppointmentConflict(ex: AppointmentConflictException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = "Time slot is already booked",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidStatusTransitionException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleInvalidStatusTransition(ex: InvalidStatusTransitionException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 422,
            message = ex.message ?: "Invalid status transition",
            path = request.requestURI,
        )

    @ExceptionHandler(CancellationWindowExpiredException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleCancellationWindowExpired(ex: CancellationWindowExpiredException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 422,
            message = ex.message ?: "Cancellation window expired",
            path = request.requestURI,
        )

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDenied(ex: AccessDeniedException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 403,
            message = ex.message ?: "Access denied",
            path = request.requestURI,
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ErrorResponse {
        val message = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = message,
            path = request.requestURI,
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Bad request",
            path = request.requestURI,
        )

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleIllegalState(ex: IllegalStateException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 422,
            message = ex.message ?: "Invalid state",
            path = request.requestURI,
        )

    @ExceptionHandler(EmployeeNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleEmployeeNotFound(ex: EmployeeNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Employee not found",
            path = request.requestURI,
        )

    @ExceptionHandler(ScheduleOverlapException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleScheduleOverlap(ex: ScheduleOverlapException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "Schedule overlap",
            path = request.requestURI,
        )

    @ExceptionHandler(ScheduleExceptionOverlapException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleScheduleExceptionOverlap(ex: ScheduleExceptionOverlapException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "Schedule exception overlap",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidScheduleException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidSchedule(ex: InvalidScheduleException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Invalid schedule",
            path = request.requestURI,
        )

    @ExceptionHandler(CannotDeleteActiveEmployeeException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleCannotDeleteActiveEmployee(ex: CannotDeleteActiveEmployeeException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 422,
            message = ex.message ?: "Cannot delete active employee",
            path = request.requestURI,
        )
}

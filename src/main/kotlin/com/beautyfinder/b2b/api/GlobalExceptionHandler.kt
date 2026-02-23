package com.beautyfinder.b2b.api

import com.beautyfinder.b2b.domain.service.CannotArchiveServiceWithActiveAppointmentsException
import com.beautyfinder.b2b.domain.service.DuplicateCategoryNameException
import com.beautyfinder.b2b.domain.service.DuplicateServiceNameException
import com.beautyfinder.b2b.domain.service.InvalidDurationException
import com.beautyfinder.b2b.domain.service.InvalidPriceException
import com.beautyfinder.b2b.domain.service.ServiceCategoryNotFoundException
import com.beautyfinder.b2b.domain.service.ServiceNotFoundException
import com.beautyfinder.b2b.domain.service.ServiceVariantNotFoundException
import com.beautyfinder.b2b.domain.salon.InvalidCancellationWindowException
import com.beautyfinder.b2b.domain.salon.InvalidLoyaltyConfigException
import com.beautyfinder.b2b.domain.salon.InvalidOpeningHoursException
import com.beautyfinder.b2b.domain.salon.InvalidSlotIntervalException
import com.beautyfinder.b2b.domain.salon.InvalidTimezoneException
import com.beautyfinder.b2b.domain.salon.SalonNotFoundException
import com.beautyfinder.b2b.domain.salon.SalonSlugAlreadyExistsException
import com.beautyfinder.b2b.domain.billing.BillingDomainException
import com.beautyfinder.b2b.domain.billing.InvoiceAlreadyPaidException
import com.beautyfinder.b2b.domain.billing.InvoiceNotFoundException
import com.beautyfinder.b2b.domain.billing.InvalidBillingPeriodException
import com.beautyfinder.b2b.domain.billing.NoActiveSubscriptionException
import com.beautyfinder.b2b.domain.billing.ReportAlreadyGeneratedException
import com.beautyfinder.b2b.domain.client.ClientAlreadyExistsException
import com.beautyfinder.b2b.domain.client.ClientBlockedException
import com.beautyfinder.b2b.domain.client.ClientNotFoundException
import com.beautyfinder.b2b.domain.client.CsvImportException
import com.beautyfinder.b2b.domain.client.GdprConsentAlreadyGrantedException
import com.beautyfinder.b2b.domain.client.InsufficientLoyaltyPointsException
import com.beautyfinder.b2b.domain.client.SensitiveDataNotFoundException
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
import jakarta.validation.ConstraintViolationException
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

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: HttpServletRequest): ErrorResponse {
        val message = ex.constraintViolations.joinToString(", ") { "${it.propertyPath}: ${it.message}" }
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

    // -- Salon Settings Module Exceptions --

    @ExceptionHandler(SalonNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleSalonNotFound(ex: SalonNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Salon not found",
            path = request.requestURI,
        )

    @ExceptionHandler(SalonSlugAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleSalonSlugAlreadyExists(ex: SalonSlugAlreadyExistsException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "Salon slug already exists",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidOpeningHoursException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidOpeningHours(ex: InvalidOpeningHoursException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Invalid opening hours",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidLoyaltyConfigException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidLoyaltyConfig(ex: InvalidLoyaltyConfigException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Invalid loyalty configuration",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidTimezoneException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidTimezone(ex: InvalidTimezoneException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Invalid timezone",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidCancellationWindowException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidCancellationWindow(ex: InvalidCancellationWindowException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Invalid cancellation window",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidSlotIntervalException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidSlotInterval(ex: InvalidSlotIntervalException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Invalid slot interval. Allowed values: 5, 10, 15, 20, 30, 60",
            path = request.requestURI,
        )

    // -- CRM Module Exceptions --

    @ExceptionHandler(ClientNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleClientNotFound(ex: ClientNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Client not found",
            path = request.requestURI,
        )

    @ExceptionHandler(SensitiveDataNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleSensitiveDataNotFound(ex: SensitiveDataNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Sensitive data not found",
            path = request.requestURI,
        )

    @ExceptionHandler(ClientAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleClientAlreadyExists(ex: ClientAlreadyExistsException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = "Client with this phone already exists",
            path = request.requestURI,
        )

    @ExceptionHandler(ClientBlockedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleClientBlocked(ex: ClientBlockedException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 403,
            message = ex.message ?: "Client is blacklisted",
            path = request.requestURI,
        )

    @ExceptionHandler(InsufficientLoyaltyPointsException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleInsufficientLoyaltyPoints(ex: InsufficientLoyaltyPointsException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 422,
            message = ex.message ?: "Insufficient loyalty points",
            path = request.requestURI,
        )

    @ExceptionHandler(GdprConsentAlreadyGrantedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleGdprConsentAlreadyGranted(ex: GdprConsentAlreadyGrantedException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "GDPR consent already granted",
            path = request.requestURI,
        )

    @ExceptionHandler(CsvImportException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleCsvImportException(ex: CsvImportException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "CSV import error",
            path = request.requestURI,
        )

    // -- Billing Module Exceptions --

    @ExceptionHandler(ReportAlreadyGeneratedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleReportAlreadyGenerated(ex: ReportAlreadyGeneratedException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "Report already generated",
            path = request.requestURI,
        )

    @ExceptionHandler(InvoiceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleInvoiceNotFound(ex: InvoiceNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Invoice not found",
            path = request.requestURI,
        )

    @ExceptionHandler(InvoiceAlreadyPaidException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleInvoiceAlreadyPaid(ex: InvoiceAlreadyPaidException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "Invoice already paid",
            path = request.requestURI,
        )

    @ExceptionHandler(NoActiveSubscriptionException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleNoActiveSubscription(ex: NoActiveSubscriptionException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 422,
            message = ex.message ?: "No active subscription",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidBillingPeriodException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidBillingPeriod(ex: InvalidBillingPeriodException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Invalid billing period",
            path = request.requestURI,
        )

    @ExceptionHandler(BillingDomainException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleBillingDomainException(ex: BillingDomainException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "Billing error",
            path = request.requestURI,
        )

    // -- Service Module Exceptions --

    @ExceptionHandler(ServiceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleServiceNotFound(ex: ServiceNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Service not found",
            path = request.requestURI,
        )

    @ExceptionHandler(ServiceVariantNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleServiceVariantNotFound(ex: ServiceVariantNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Service variant not found",
            path = request.requestURI,
        )

    @ExceptionHandler(ServiceCategoryNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleServiceCategoryNotFound(ex: ServiceCategoryNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Service category not found",
            path = request.requestURI,
        )

    @ExceptionHandler(DuplicateServiceNameException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDuplicateServiceName(ex: DuplicateServiceNameException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "Duplicate service name",
            path = request.requestURI,
        )

    @ExceptionHandler(DuplicateCategoryNameException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDuplicateCategoryName(ex: DuplicateCategoryNameException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 409,
            message = ex.message ?: "Duplicate category name",
            path = request.requestURI,
        )

    @ExceptionHandler(CannotArchiveServiceWithActiveAppointmentsException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleCannotArchiveService(ex: CannotArchiveServiceWithActiveAppointmentsException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 422,
            message = ex.message ?: "Cannot archive service with active appointments",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidDurationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidDuration(ex: InvalidDurationException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = "Duration must be multiple of 5, between 5 and 480 minutes",
            path = request.requestURI,
        )

    @ExceptionHandler(InvalidPriceException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidPrice(ex: InvalidPriceException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Invalid price",
            path = request.requestURI,
        )
}

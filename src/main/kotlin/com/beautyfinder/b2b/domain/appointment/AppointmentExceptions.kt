package com.beautyfinder.b2b.domain.appointment

import java.time.OffsetDateTime
import java.util.UUID

open class AppointmentDomainException(message: String) : RuntimeException(message)

class EmployeeNotAvailableException(employeeId: UUID, slotStart: OffsetDateTime) :
    AppointmentDomainException("Employee $employeeId is not available at $slotStart")

class AppointmentConflictException(employeeId: UUID, slotStart: OffsetDateTime) :
    AppointmentDomainException("Employee $employeeId already has a booking at $slotStart")

class InvalidStatusTransitionException(from: AppointmentStatus, to: AppointmentStatus) :
    AppointmentDomainException("Invalid status transition: $from → $to")

class CancellationWindowExpiredException(appointmentId: UUID, windowHours: Int) :
    AppointmentDomainException("Cancellation window of ${windowHours}h has expired for appointment $appointmentId")

class AppointmentNotFoundException(id: UUID) :
    AppointmentDomainException("Appointment $id not found")

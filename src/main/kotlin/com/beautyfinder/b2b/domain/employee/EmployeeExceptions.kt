package com.beautyfinder.b2b.domain.employee

import java.time.LocalTime
import java.time.DayOfWeek
import java.util.UUID

open class EmployeeDomainException(message: String) : RuntimeException(message)

class EmployeeNotFoundException(id: UUID) :
    EmployeeDomainException("Employee $id not found")

class EmployeeNotInSalonException(id: UUID, salonId: UUID) :
    EmployeeDomainException("Employee $id does not belong to salon $salonId")

class ScheduleOverlapException(employeeId: UUID, day: DayOfWeek, existingStart: LocalTime, existingEnd: LocalTime) :
    EmployeeDomainException("Employee $employeeId already has schedule for $day: $existingStart-$existingEnd")

class InvalidScheduleException(message: String) :
    EmployeeDomainException(message)

class ScheduleExceptionOverlapException(employeeId: UUID, startAt: java.time.OffsetDateTime, endAt: java.time.OffsetDateTime) :
    EmployeeDomainException("Schedule exception overlaps existing for employee $employeeId: $startAt - $endAt")

class CannotDeleteActiveEmployeeException(id: UUID, futureAppointmentCount: Int) :
    EmployeeDomainException("Employee $id has $futureAppointmentCount future appointments")

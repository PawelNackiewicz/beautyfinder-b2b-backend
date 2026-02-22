package com.beautyfinder.b2b.application.employee

import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.appointment.TimeSlot
import com.beautyfinder.b2b.domain.employee.CannotDeleteActiveEmployeeException
import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.employee.EmployeeNotFoundException
import com.beautyfinder.b2b.domain.employee.EmployeeStatus
import com.beautyfinder.b2b.domain.schedule.ScheduleException
import com.beautyfinder.b2b.domain.schedule.ScheduleExceptionType
import com.beautyfinder.b2b.domain.schedule.ScheduleValidator
import com.beautyfinder.b2b.domain.schedule.WeeklySchedule
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.ScheduleExceptionRepository
import com.beautyfinder.b2b.infrastructure.UserRepository
import com.beautyfinder.b2b.infrastructure.WeeklyScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val weeklyScheduleRepository: WeeklyScheduleRepository,
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val appointmentRepository: AppointmentRepository,
    private val userRepository: UserRepository,
    private val employeeMapper: EmployeeMapper,
) {

    private val log = LoggerFactory.getLogger(EmployeeService::class.java)

    fun listEmployees(salonId: UUID, includeInactive: Boolean = false): List<EmployeeDto> {
        val employees = if (includeInactive) {
            employeeRepository.findAllBySalonIdAndStatusNotOrderByDisplayNameAsc(salonId, EmployeeStatus.DELETED)
        } else {
            employeeRepository.findAllBySalonIdAndStatusOrderByDisplayNameAsc(salonId, EmployeeStatus.ACTIVE)
        }
        return employeeMapper.toDtoList(employees)
    }

    fun getEmployee(id: UUID, salonId: UUID): EmployeeDto {
        val employee = employeeRepository.findByIdAndSalonId(id, salonId)
            ?: throw EmployeeNotFoundException(id)
        return employeeMapper.toDto(employee)
    }

    @Transactional
    fun createEmployee(request: CreateEmployeeRequest, salonId: UUID): EmployeeDto {
        require(userRepository.findById(request.userId).isPresent) {
            "User ${request.userId} not found"
        }
        val existingUser = userRepository.findById(request.userId).get()
        require(existingUser.salonId == salonId) {
            "User ${request.userId} does not belong to this salon"
        }

        require(!employeeRepository.existsByUserIdAndSalonIdAndStatusNot(request.userId, salonId, EmployeeStatus.DELETED)) {
            "An active employee already exists for user ${request.userId} in this salon"
        }

        val employee = Employee(
            salonId = salonId,
            userId = request.userId,
            displayName = request.displayName,
            phone = request.phone,
            avatarUrl = request.avatarUrl,
            color = request.color,
            status = EmployeeStatus.ACTIVE,
            serviceIds = request.serviceIds.toMutableSet(),
        )

        val saved = employeeRepository.save(employee)
        log.info("Created employee {} for salon {}", saved.id, salonId)

        request.weeklySchedule?.let { schedules ->
            upsertWeeklyScheduleInternal(saved.id!!, schedules, salonId)
        }

        return employeeMapper.toDto(saved)
    }

    @Transactional
    fun updateEmployee(id: UUID, request: UpdateEmployeeRequest, salonId: UUID): EmployeeDto {
        val employee = employeeRepository.findByIdAndSalonId(id, salonId)
            ?: throw EmployeeNotFoundException(id)

        request.displayName?.let { employee.displayName = it }
        request.phone?.let { employee.phone = it }
        request.avatarUrl?.let { employee.avatarUrl = it }
        request.color?.let { employee.color = it }
        request.serviceIds?.let { employee.serviceIds = it.toMutableSet() }

        val saved = employeeRepository.save(employee)
        return employeeMapper.toDto(saved)
    }

    @Transactional
    fun deactivateEmployee(id: UUID, salonId: UUID) {
        val employee = employeeRepository.findByIdAndSalonId(id, salonId)
            ?: throw EmployeeNotFoundException(id)

        val now = OffsetDateTime.now()
        val futureAppointments = appointmentRepository.findConflictingAppointments(
            employeeId = id,
            startAt = now,
            endAt = now.plusYears(10),
            statuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED),
        )

        if (futureAppointments.isNotEmpty()) {
            log.warn("Attempted to deactivate employee {} with {} future appointments", id, futureAppointments.size)
            throw CannotDeleteActiveEmployeeException(id, futureAppointments.size)
        }

        employee.status = EmployeeStatus.INACTIVE
        employeeRepository.save(employee)
        // Keep WeeklySchedule so it can be restored if employee is reactivated
        log.info("Deactivated employee {}", id)
    }

    fun getWeeklySchedule(employeeId: UUID, salonId: UUID): List<WeeklyScheduleDto> {
        val employee = employeeRepository.findByIdAndSalonId(employeeId, salonId)
            ?: throw EmployeeNotFoundException(employeeId)

        val existing = weeklyScheduleRepository.findAllByEmployeeId(employee.id!!)
        val existingByDay = existing.associateBy { it.dayOfWeek }

        return DayOfWeek.entries.map { day ->
            existingByDay[day]?.let { employeeMapper.toScheduleDto(it) }
                ?: WeeklyScheduleDto(
                    id = null,
                    employeeId = employeeId,
                    dayOfWeek = day,
                    startTime = LocalTime.of(0, 0),
                    endTime = LocalTime.of(0, 0),
                    isWorkingDay = false,
                )
        }
    }

    @Transactional
    fun upsertWeeklySchedule(
        employeeId: UUID,
        schedules: List<UpsertScheduleRequest>,
        salonId: UUID,
    ): List<WeeklyScheduleDto> {
        val employee = employeeRepository.findByIdAndSalonId(employeeId, salonId)
            ?: throw EmployeeNotFoundException(employeeId)

        return upsertWeeklyScheduleInternal(employee.id!!, schedules, salonId)
    }

    private fun upsertWeeklyScheduleInternal(
        employeeId: UUID,
        schedules: List<UpsertScheduleRequest>,
        salonId: UUID,
    ): List<WeeklyScheduleDto> {
        val entities = schedules.filter { it.isWorkingDay }.map { req ->
            WeeklySchedule(
                employeeId = employeeId,
                salonId = salonId,
                dayOfWeek = req.dayOfWeek,
                startTime = requireNotNull(req.startTime) { "startTime is required for working days" },
                endTime = requireNotNull(req.endTime) { "endTime is required for working days" },
                isWorkingDay = true,
            ).also { schedule ->
                ScheduleValidator.validateWeeklySchedule(schedule).getOrThrow()
            }
        }

        weeklyScheduleRepository.deleteAllByEmployeeId(employeeId)
        val saved = weeklyScheduleRepository.saveAll(entities)

        // Return all 7 days, filling non-working days
        val savedByDay = saved.associateBy { it.dayOfWeek }
        return DayOfWeek.entries.map { day ->
            savedByDay[day]?.let { employeeMapper.toScheduleDto(it) }
                ?: WeeklyScheduleDto(
                    id = null,
                    employeeId = employeeId,
                    dayOfWeek = day,
                    startTime = LocalTime.of(0, 0),
                    endTime = LocalTime.of(0, 0),
                    isWorkingDay = false,
                )
        }
    }

    @Transactional
    fun addScheduleException(
        employeeId: UUID,
        request: AddExceptionRequest,
        salonId: UUID,
    ): ScheduleExceptionDto {
        val employee = employeeRepository.findByIdAndSalonId(employeeId, salonId)
            ?: throw EmployeeNotFoundException(employeeId)

        val empId = employee.id!!
        val existing = scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(
            employeeId = empId,
            endAt = request.endAt,
            startAt = request.startAt,
        )

        val exception = ScheduleException(
            employeeId = empId,
            salonId = salonId,
            startAt = request.startAt,
            endAt = request.endAt,
            reason = request.reason,
            type = request.type,
        )

        ScheduleValidator.validateScheduleException(exception, existing).getOrThrow()

        val saved = scheduleExceptionRepository.save(exception)
        return employeeMapper.toExceptionDto(saved)
    }

    fun listScheduleExceptions(
        employeeId: UUID,
        salonId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<ScheduleExceptionDto> {
        val employee = employeeRepository.findByIdAndSalonId(employeeId, salonId)
            ?: throw EmployeeNotFoundException(employeeId)

        val fromDateTime = from.atStartOfDay().atOffset(ZoneOffset.UTC)
        val toDateTime = to.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)

        val exceptions = scheduleExceptionRepository
            .findAllByEmployeeIdAndSalonIdAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
                employee.id!!, salonId, fromDateTime, toDateTime,
            )

        return employeeMapper.toExceptionDtoList(exceptions.sortedBy { it.startAt })
    }

    @Transactional
    fun deleteScheduleException(exceptionId: UUID, employeeId: UUID, salonId: UUID) {
        val exception = scheduleExceptionRepository.findByIdAndEmployeeIdAndSalonId(exceptionId, employeeId, salonId)
            ?: throw com.beautyfinder.b2b.domain.employee.EmployeeNotFoundException(exceptionId)
        scheduleExceptionRepository.delete(exception)
    }

    fun getAvailableSlots(
        employeeId: UUID,
        date: LocalDate,
        variantDurationMinutes: Int,
        salonId: UUID,
    ): List<AvailableSlotDto> {
        val employee = employeeRepository.findByIdAndSalonId(employeeId, salonId)
            ?: throw EmployeeNotFoundException(employeeId)
        val empId = employee.id!!

        // 1) Get weekly schedule for this day
        val schedule = weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(empId, date.dayOfWeek)
        if (schedule == null || !schedule.isWorkingDay) return emptyList()

        // 2) Check schedule exceptions for this day
        val dayStart = date.atStartOfDay().atOffset(ZoneOffset.UTC)
        val dayEnd = date.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)

        val exceptions = scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(
            employeeId = empId,
            endAt = dayEnd,
            startAt = dayStart,
        )

        // If an exception covers the entire working day, no slots
        val workStart = date.atTime(schedule.startTime).atOffset(ZoneOffset.UTC)
        val workEnd = date.atTime(schedule.endTime).atOffset(ZoneOffset.UTC)
        val fullDayBlocked = exceptions.any { ex ->
            !ex.startAt.isAfter(workStart) && !ex.endAt.isBefore(workEnd)
        }
        if (fullDayBlocked) return emptyList()

        // 3) Get existing appointments for this day
        val activeStatuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)
        val appointments = appointmentRepository.findConflictingAppointments(
            employeeId = empId,
            startAt = workStart,
            endAt = workEnd,
            statuses = activeStatuses,
        )

        // 4) Generate 15-minute interval slots
        val slots = mutableListOf<AvailableSlotDto>()
        val now = OffsetDateTime.now()
        val bufferTime = now.plusHours(1)

        var slotStart = workStart
        while (slotStart.plusMinutes(variantDurationMinutes.toLong()) <= workEnd) {
            val slotEnd = slotStart.plusMinutes(variantDurationMinutes.toLong())
            val slot = TimeSlot(slotStart, slotEnd)

            // Filter past slots (must be at least 1h in the future)
            val isInFuture = slotStart.isAfter(bufferTime)

            // Check no appointment conflict
            val noAppointmentConflict = appointments.none { appt ->
                TimeSlot(appt.startAt, appt.endAt).overlaps(slot)
            }

            // Check no schedule exception conflict
            val noExceptionConflict = exceptions.none { ex ->
                TimeSlot(ex.startAt, ex.endAt).overlaps(slot)
            }

            if (isInFuture && noAppointmentConflict && noExceptionConflict) {
                slots.add(AvailableSlotDto(
                    start = slotStart,
                    end = slotEnd,
                    durationMinutes = variantDurationMinutes,
                ))
            }

            slotStart = slotStart.plusMinutes(15)
        }

        return slots
    }
}

// --- Service-layer request DTOs ---

data class CreateEmployeeRequest(
    val userId: UUID,
    val displayName: String,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val color: String? = null,
    val serviceIds: Set<UUID> = emptySet(),
    val weeklySchedule: List<UpsertScheduleRequest>? = null,
)

data class UpdateEmployeeRequest(
    val displayName: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val color: String? = null,
    val serviceIds: Set<UUID>? = null,
)

data class UpsertScheduleRequest(
    val dayOfWeek: DayOfWeek,
    val isWorkingDay: Boolean,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
)

data class AddExceptionRequest(
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val reason: String? = null,
    val type: ScheduleExceptionType,
)

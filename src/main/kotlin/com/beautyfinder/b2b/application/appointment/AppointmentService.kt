package com.beautyfinder.b2b.application.appointment

import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentConflictException
import com.beautyfinder.b2b.domain.appointment.AppointmentNotFoundException
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.appointment.AppointmentStatusMachine
import com.beautyfinder.b2b.domain.appointment.CancellationWindowExpiredException
import com.beautyfinder.b2b.domain.appointment.EmployeeNotAvailableException
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.SalonRepository
import com.beautyfinder.b2b.infrastructure.ScheduleExceptionRepository
import com.beautyfinder.b2b.infrastructure.ServiceVariantRepository
import com.beautyfinder.b2b.infrastructure.WeeklyScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val serviceVariantRepository: ServiceVariantRepository,
    private val weeklyScheduleRepository: WeeklyScheduleRepository,
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val salonRepository: SalonRepository,
    private val appointmentMapper: AppointmentMapper,
) {

    private val log = LoggerFactory.getLogger(AppointmentService::class.java)

    fun getAppointments(query: AppointmentQuery, salonId: UUID): List<AppointmentDto> {
        val from = query.date.atStartOfDay().atOffset(ZoneOffset.UTC)
        val to = query.date.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)

        val appointments = if (query.employeeId != null) {
            appointmentRepository.findAllBySalonIdAndEmployeeIdAndStartAtBetweenOrderByStartAtAsc(
                salonId, query.employeeId, from, to,
            )
        } else {
            appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, from, to)
        }

        val filtered = query.includeStatuses?.let { statuses ->
            appointments.filter { it.status in statuses }
        } ?: appointments

        return appointmentMapper.toDtoList(filtered)
    }

    fun getWeekAppointments(weekStart: LocalDate, salonId: UUID): Map<LocalDate, List<AppointmentDto>> {
        val from = weekStart.atStartOfDay().atOffset(ZoneOffset.UTC)
        val to = weekStart.plusDays(6).atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)

        val appointments = appointmentRepository.findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, from, to)

        return (0L..6L).associate { dayOffset ->
            val date = weekStart.plusDays(dayOffset)
            val dayAppointments = appointments.filter { it.startAt.toLocalDate() == date }
            date to appointmentMapper.toDtoList(dayAppointments)
        }
    }

    @Transactional
    fun createAppointment(request: CreateAppointmentRequest, salonId: UUID): AppointmentDto {
        val variant = serviceVariantRepository.findById(request.variantId)
            .orElseThrow { IllegalArgumentException("ServiceVariant ${request.variantId} not found") }

        val endAt = request.startAt.plusMinutes(variant.durationMinutes.toLong())

        val employee = employeeRepository.findByIdAndSalonId(request.employeeId, salonId)
            ?: throw EmployeeNotAvailableException(request.employeeId, request.startAt)

        validateEmployeeAvailability(employee.id!!, request.startAt, endAt)

        val conflicts = appointmentRepository.findConflictingAppointments(
            employeeId = request.employeeId,
            startAt = request.startAt,
            endAt = endAt,
            statuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS),
        )
        if (conflicts.isNotEmpty()) {
            log.warn("Appointment conflict for employee {} at {}", request.employeeId, request.startAt)
            throw AppointmentConflictException(request.employeeId, request.startAt)
        }

        val finalPrice = variant.price
        val commissionValue = if (request.source == AppointmentSource.MARKETPLACE) {
            finalPrice.multiply(BigDecimal("0.15"))
        } else {
            null
        }

        val appointment = Appointment(
            salonId = salonId,
            clientId = request.clientId,
            employeeId = request.employeeId,
            variantId = request.variantId,
            startAt = request.startAt,
            endAt = endAt,
            status = AppointmentStatus.SCHEDULED,
            finalPrice = finalPrice,
            commissionValue = commissionValue,
            source = request.source,
            notes = request.notes,
        )

        val saved = appointmentRepository.save(appointment)
        log.info("Created appointment {} for salon {} employee {}", saved.id, salonId, request.employeeId)
        return appointmentMapper.toDto(saved)
    }

    @Transactional
    fun updateAppointmentStatus(
        id: UUID,
        newStatus: AppointmentStatus,
        reason: String?,
        salonId: UUID,
    ): AppointmentDto {
        val appointment = appointmentRepository.findByIdAndSalonId(id, salonId)
            ?: throw AppointmentNotFoundException(id)

        AppointmentStatusMachine.validateTransition(appointment.status, newStatus)
            .getOrThrow()

        if (newStatus == AppointmentStatus.CANCELLED && appointment.source == AppointmentSource.MARKETPLACE) {
            val salon = salonRepository.findById(salonId).orElseThrow()
            val windowDeadline = OffsetDateTime.now().plusHours(salon.cancellationWindowHours.toLong())
            if (appointment.startAt.isBefore(windowDeadline)) {
                throw CancellationWindowExpiredException(id, salon.cancellationWindowHours)
            }
        }

        appointment.status = newStatus

        if (newStatus == AppointmentStatus.CANCELLED) {
            appointment.cancellationReason = reason
        }

        if (newStatus == AppointmentStatus.COMPLETED && appointment.commissionValue == null
            && appointment.source == AppointmentSource.MARKETPLACE
        ) {
            appointment.finalPrice?.let {
                appointment.commissionValue = it.multiply(BigDecimal("0.15"))
            }
        }

        val saved = appointmentRepository.save(appointment)
        return appointmentMapper.toDto(saved)
    }

    @Transactional
    fun rescheduleAppointment(id: UUID, newStartAt: OffsetDateTime, salonId: UUID): AppointmentDto {
        val appointment = appointmentRepository.findByIdAndSalonId(id, salonId)
            ?: throw AppointmentNotFoundException(id)

        check(appointment.status in listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)) {
            "Can only reschedule SCHEDULED or CONFIRMED appointments"
        }

        val variant = serviceVariantRepository.findById(appointment.variantId).orElseThrow()
        val newEndAt = newStartAt.plusMinutes(variant.durationMinutes.toLong())

        validateEmployeeAvailability(appointment.employeeId, newStartAt, newEndAt)

        val conflicts = appointmentRepository.findConflictingAppointments(
            employeeId = appointment.employeeId,
            startAt = newStartAt,
            endAt = newEndAt,
            statuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS),
            excludeId = appointment.id!!,
        )
        if (conflicts.isNotEmpty()) {
            throw AppointmentConflictException(appointment.employeeId, newStartAt)
        }

        appointment.startAt = newStartAt
        appointment.endAt = newEndAt

        val saved = appointmentRepository.save(appointment)
        return appointmentMapper.toDto(saved)
    }

    fun getAppointmentById(id: UUID, salonId: UUID): AppointmentDto {
        val appointment = appointmentRepository.findByIdAndSalonId(id, salonId)
            ?: throw AppointmentNotFoundException(id)
        return appointmentMapper.toDto(appointment)
    }

    @Transactional
    fun autoCompleteAppointments(now: OffsetDateTime): Int {
        val statuses = listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS)
        val pastAppointments = appointmentRepository.findAllByStatusInAndEndAtBefore(statuses, now)

        pastAppointments.forEach { it.status = AppointmentStatus.COMPLETED }
        appointmentRepository.saveAll(pastAppointments)

        return pastAppointments.size
    }

    private fun validateEmployeeAvailability(
        employeeId: UUID,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ) {
        val dayOfWeek = startAt.dayOfWeek
        val schedule = weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek)
            ?: throw EmployeeNotAvailableException(employeeId, startAt)

        val slotStartTime = startAt.toLocalTime()
        val slotEndTime = endAt.toLocalTime()

        if (slotStartTime.isBefore(schedule.startTime) || slotEndTime.isAfter(schedule.endTime)) {
            throw EmployeeNotAvailableException(employeeId, startAt)
        }

        val exceptions = scheduleExceptionRepository
            .findByEmployeeIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(employeeId, endAt, startAt)

        if (exceptions.isNotEmpty()) {
            throw EmployeeNotAvailableException(employeeId, startAt)
        }
    }
}

data class CreateAppointmentRequest(
    val clientId: UUID,
    val employeeId: UUID,
    val variantId: UUID,
    val startAt: OffsetDateTime,
    val source: AppointmentSource = AppointmentSource.DIRECT,
    val notes: String? = null,
)

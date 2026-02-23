package com.beautyfinder.b2b.application.appointment

import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.service.ServiceVariant
import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentConflictException
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.appointment.CancellationWindowExpiredException
import com.beautyfinder.b2b.domain.appointment.EmployeeNotAvailableException
import com.beautyfinder.b2b.domain.appointment.InvalidStatusTransitionException
import com.beautyfinder.b2b.domain.schedule.WeeklySchedule
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.salon.SalonRepository
import com.beautyfinder.b2b.infrastructure.ScheduleExceptionRepository
import com.beautyfinder.b2b.infrastructure.service.ServiceVariantRepository
import com.beautyfinder.b2b.infrastructure.WeeklyScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class AppointmentServiceTest {

    private val appointmentRepository = mockk<AppointmentRepository>(relaxed = true)
    private val employeeRepository = mockk<EmployeeRepository>()
    private val serviceVariantRepository = mockk<ServiceVariantRepository>()
    private val weeklyScheduleRepository = mockk<WeeklyScheduleRepository>()
    private val scheduleExceptionRepository = mockk<ScheduleExceptionRepository>()
    private val salonRepository = mockk<SalonRepository>()
    private val appointmentMapper = mockk<AppointmentMapper>()

    private val service = AppointmentService(
        appointmentRepository = appointmentRepository,
        employeeRepository = employeeRepository,
        serviceVariantRepository = serviceVariantRepository,
        weeklyScheduleRepository = weeklyScheduleRepository,
        scheduleExceptionRepository = scheduleExceptionRepository,
        salonRepository = salonRepository,
        appointmentMapper = appointmentMapper,
    )

    private val salonId = UUID.randomUUID()
    private val employeeId = UUID.randomUUID()
    private val clientId = UUID.randomUUID()
    private val variantId = UUID.randomUUID()

    // Monday 10:00 UTC
    private val startAt = OffsetDateTime.of(2025, 1, 13, 10, 0, 0, 0, ZoneOffset.UTC)

    // --- Helpers ---

    private fun buildServiceVariant(
        id: UUID = variantId,
        durationMinutes: Int = 60,
        price: BigDecimal = BigDecimal("100.00"),
    ) = ServiceVariant(
        serviceId = UUID.randomUUID(),
        salonId = UUID.randomUUID(),
        name = "Standard",
        durationMinutes = durationMinutes,
        price = price,
    ).also {
        val idField = com.beautyfinder.b2b.domain.BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(it, id)
    }

    private fun buildEmployee(id: UUID = employeeId, salonId: UUID = this.salonId) =
        Employee(salonId = salonId, userId = UUID.randomUUID(), displayName = "John").also {
            val idField = com.beautyfinder.b2b.domain.BaseEntity::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(it, id)
        }

    private fun buildAppointment(
        id: UUID = UUID.randomUUID(),
        salonId: UUID = this.salonId,
        employeeId: UUID = this.employeeId,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED,
        source: AppointmentSource = AppointmentSource.DIRECT,
        startAt: OffsetDateTime = this.startAt,
        endAt: OffsetDateTime = startAt.plusMinutes(60),
        finalPrice: BigDecimal? = BigDecimal("100.00"),
        commissionValue: BigDecimal? = null,
    ) = Appointment(
        salonId = salonId,
        clientId = clientId,
        employeeId = employeeId,
        variantId = variantId,
        startAt = startAt,
        endAt = endAt,
        status = status,
        source = source,
        finalPrice = finalPrice,
        commissionValue = commissionValue,
    ).also {
        val idField = com.beautyfinder.b2b.domain.BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(it, id)
    }

    private fun buildWeeklySchedule(
        employeeId: UUID = this.employeeId,
        dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        startTime: LocalTime = LocalTime.of(8, 0),
        endTime: LocalTime = LocalTime.of(18, 0),
    ) = WeeklySchedule(
        employeeId = employeeId,
        salonId = salonId,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
    )

    private fun buildSalon(cancellationWindowHours: Int = 24) =
        Salon(name = "Test Salon", slug = "test-salon", cancellationWindowHours = cancellationWindowHours).also {
            val idField = com.beautyfinder.b2b.domain.BaseEntity::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(it, salonId)
        }

    private fun setupHappyPath() {
        every { serviceVariantRepository.findById(variantId) } returns Optional.of(buildServiceVariant())
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns buildEmployee()
        every { weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, DayOfWeek.MONDAY) } returns buildWeeklySchedule()
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(any(), any(), any()) } returns emptyList()
        every { appointmentRepository.findConflictingAppointments(any(), any(), any(), any<List<AppointmentStatus>>()) } returns emptyList()
        every { appointmentRepository.save(any()) } answers { firstArg() }
        every { appointmentMapper.toDto(any()) } answers {
            val a = firstArg<Appointment>()
            AppointmentDto(
                id = a.id ?: UUID.randomUUID(),
                salonId = a.salonId,
                clientId = a.clientId,
                clientName = "Client",
                employeeId = a.employeeId,
                employeeName = "Employee",
                variantId = a.variantId,
                variantName = "Variant",
                serviceName = "Service",
                startAt = a.startAt,
                endAt = a.endAt,
                status = a.status,
                source = a.source,
                finalPrice = a.finalPrice,
                commissionValue = a.commissionValue,
                notes = a.notes,
                cancellationReason = a.cancellationReason,
                createdAt = a.createdAt,
                updatedAt = a.updatedAt,
            )
        }
    }

    // --- Tests ---

    @Test
    fun `createAppointment - happy path, DIRECT source, no commission`() {
        // given
        setupHappyPath()
        val request = CreateAppointmentRequest(
            clientId = clientId,
            employeeId = employeeId,
            variantId = variantId,
            startAt = startAt,
            source = AppointmentSource.DIRECT,
        )

        // when
        val result = service.createAppointment(request, salonId)

        // then
        verify { appointmentRepository.save(any()) }
        assertNull(result.commissionValue)
        assertEquals(AppointmentStatus.SCHEDULED, result.status)
    }

    @Test
    fun `createAppointment - MARKETPLACE source, commission calculated`() {
        // given
        setupHappyPath()
        val request = CreateAppointmentRequest(
            clientId = clientId,
            employeeId = employeeId,
            variantId = variantId,
            startAt = startAt,
            source = AppointmentSource.MARKETPLACE,
        )

        // when
        val result = service.createAppointment(request, salonId)

        // then
        assertEquals(0, BigDecimal("15.00").compareTo(result.commissionValue))
    }

    @Test
    fun `createAppointment - employee conflict throws AppointmentConflictException`() {
        // given
        setupHappyPath()
        every { appointmentRepository.findConflictingAppointments(any(), any(), any(), any<List<AppointmentStatus>>()) } returns
            listOf(buildAppointment())

        val request = CreateAppointmentRequest(
            clientId = clientId,
            employeeId = employeeId,
            variantId = variantId,
            startAt = startAt,
        )

        // when/then
        assertThrows<AppointmentConflictException> {
            service.createAppointment(request, salonId)
        }
    }

    @Test
    fun `createAppointment - no weekly schedule throws EmployeeNotAvailableException`() {
        // given
        setupHappyPath()
        every { weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, DayOfWeek.MONDAY) } returns null

        val request = CreateAppointmentRequest(
            clientId = clientId,
            employeeId = employeeId,
            variantId = variantId,
            startAt = startAt,
        )

        // when/then
        assertThrows<EmployeeNotAvailableException> {
            service.createAppointment(request, salonId)
        }
    }

    @Test
    fun `createAppointment - schedule exception blocks throws EmployeeNotAvailableException`() {
        // given
        setupHappyPath()
        val scheduleException = com.beautyfinder.b2b.domain.schedule.ScheduleException(
            employeeId = employeeId,
            salonId = salonId,
            startAt = startAt.minusHours(1),
            endAt = startAt.plusHours(2),
            reason = "Vacation",
            type = com.beautyfinder.b2b.domain.schedule.ScheduleExceptionType.VACATION,
        )
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(any(), any(), any()) } returns
            listOf(scheduleException)

        val request = CreateAppointmentRequest(
            clientId = clientId,
            employeeId = employeeId,
            variantId = variantId,
            startAt = startAt,
        )

        // when/then
        assertThrows<EmployeeNotAvailableException> {
            service.createAppointment(request, salonId)
        }
    }

    @Test
    fun `updateStatus - valid transition SCHEDULED to CONFIRMED`() {
        // given
        val appointmentId = UUID.randomUUID()
        val appointment = buildAppointment(id = appointmentId, status = AppointmentStatus.SCHEDULED)
        every { appointmentRepository.findByIdAndSalonId(appointmentId, salonId) } returns appointment
        every { appointmentRepository.save(any()) } answers { firstArg() }
        every { appointmentMapper.toDto(any()) } answers {
            val a = firstArg<Appointment>()
            AppointmentDto(
                id = a.id!!, salonId = a.salonId, clientId = a.clientId, clientName = "C",
                employeeId = a.employeeId, employeeName = "E", variantId = a.variantId,
                variantName = "V", serviceName = "S", startAt = a.startAt, endAt = a.endAt,
                status = a.status, source = a.source, finalPrice = a.finalPrice,
                commissionValue = a.commissionValue, notes = a.notes,
                cancellationReason = a.cancellationReason, createdAt = a.createdAt, updatedAt = a.updatedAt,
            )
        }

        // when
        val result = service.updateAppointmentStatus(appointmentId, AppointmentStatus.CONFIRMED, null, salonId)

        // then
        verify { appointmentRepository.save(any()) }
        assertEquals(AppointmentStatus.CONFIRMED, result.status)
    }

    @Test
    fun `updateStatus - invalid transition COMPLETED to SCHEDULED throws`() {
        // given
        val appointmentId = UUID.randomUUID()
        val appointment = buildAppointment(id = appointmentId, status = AppointmentStatus.COMPLETED)
        every { appointmentRepository.findByIdAndSalonId(appointmentId, salonId) } returns appointment

        // when/then
        assertThrows<InvalidStatusTransitionException> {
            service.updateAppointmentStatus(appointmentId, AppointmentStatus.SCHEDULED, null, salonId)
        }
    }

    @Test
    fun `updateStatus - MARKETPLACE cancellation window expired throws`() {
        // given
        val appointmentId = UUID.randomUUID()
        val soonStartAt = OffsetDateTime.now().plusMinutes(30)
        val appointment = buildAppointment(
            id = appointmentId,
            status = AppointmentStatus.SCHEDULED,
            source = AppointmentSource.MARKETPLACE,
            startAt = soonStartAt,
            endAt = soonStartAt.plusMinutes(60),
        )
        every { appointmentRepository.findByIdAndSalonId(appointmentId, salonId) } returns appointment
        every { salonRepository.findById(salonId) } returns Optional.of(buildSalon(cancellationWindowHours = 2))

        // when/then
        assertThrows<CancellationWindowExpiredException> {
            service.updateAppointmentStatus(appointmentId, AppointmentStatus.CANCELLED, "No reason", salonId)
        }
    }

    @Test
    fun `updateStatus - DIRECT source cancellation window not checked`() {
        // given
        val appointmentId = UUID.randomUUID()
        val soonStartAt = OffsetDateTime.now().plusMinutes(30)
        val appointment = buildAppointment(
            id = appointmentId,
            status = AppointmentStatus.SCHEDULED,
            source = AppointmentSource.DIRECT,
            startAt = soonStartAt,
            endAt = soonStartAt.plusMinutes(60),
        )
        every { appointmentRepository.findByIdAndSalonId(appointmentId, salonId) } returns appointment
        every { appointmentRepository.save(any()) } answers { firstArg() }
        every { appointmentMapper.toDto(any()) } answers {
            val a = firstArg<Appointment>()
            AppointmentDto(
                id = a.id!!, salonId = a.salonId, clientId = a.clientId, clientName = "C",
                employeeId = a.employeeId, employeeName = "E", variantId = a.variantId,
                variantName = "V", serviceName = "S", startAt = a.startAt, endAt = a.endAt,
                status = a.status, source = a.source, finalPrice = a.finalPrice,
                commissionValue = a.commissionValue, notes = a.notes,
                cancellationReason = a.cancellationReason, createdAt = a.createdAt, updatedAt = a.updatedAt,
            )
        }

        // when
        val result = service.updateAppointmentStatus(appointmentId, AppointmentStatus.CANCELLED, "No show", salonId)

        // then
        assertEquals(AppointmentStatus.CANCELLED, result.status)
    }

    @Test
    fun `rescheduleAppointment - success with new slot`() {
        // given
        val appointmentId = UUID.randomUUID()
        val appointment = buildAppointment(id = appointmentId, status = AppointmentStatus.SCHEDULED)
        val newStartAt = OffsetDateTime.of(2025, 1, 13, 14, 0, 0, 0, ZoneOffset.UTC) // Monday 14:00

        every { appointmentRepository.findByIdAndSalonId(appointmentId, salonId) } returns appointment
        every { serviceVariantRepository.findById(variantId) } returns Optional.of(buildServiceVariant())
        every { weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, DayOfWeek.MONDAY) } returns buildWeeklySchedule()
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(any(), any(), any()) } returns emptyList()
        every { appointmentRepository.findConflictingAppointments(any(), any(), any(), any<List<AppointmentStatus>>(), any()) } returns emptyList()
        every { appointmentRepository.save(any()) } answers { firstArg() }
        every { appointmentMapper.toDto(any()) } answers {
            val a = firstArg<Appointment>()
            AppointmentDto(
                id = a.id!!, salonId = a.salonId, clientId = a.clientId, clientName = "C",
                employeeId = a.employeeId, employeeName = "E", variantId = a.variantId,
                variantName = "V", serviceName = "S", startAt = a.startAt, endAt = a.endAt,
                status = a.status, source = a.source, finalPrice = a.finalPrice,
                commissionValue = a.commissionValue, notes = a.notes,
                cancellationReason = a.cancellationReason, createdAt = a.createdAt, updatedAt = a.updatedAt,
            )
        }

        // when
        val result = service.rescheduleAppointment(appointmentId, newStartAt, salonId)

        // then
        assertEquals(newStartAt, result.startAt)
        assertEquals(newStartAt.plusMinutes(60), result.endAt)
    }

    @Test
    fun `autoCompleteAppointments - completes past appointments`() {
        // given
        val now = OffsetDateTime.now()
        val pastAppointments = listOf(
            buildAppointment(status = AppointmentStatus.SCHEDULED, endAt = now.minusHours(2)),
            buildAppointment(status = AppointmentStatus.CONFIRMED, endAt = now.minusHours(1)),
            buildAppointment(status = AppointmentStatus.IN_PROGRESS, endAt = now.minusMinutes(30)),
        )
        every { appointmentRepository.findAllByStatusInAndEndAtBefore(any(), any()) } returns pastAppointments
        every { appointmentRepository.saveAll(any<List<Appointment>>()) } answers { firstArg() }

        // when
        val count = service.autoCompleteAppointments(now)

        // then
        assertEquals(3, count)
        pastAppointments.forEach { assertEquals(AppointmentStatus.COMPLETED, it.status) }
        verify { appointmentRepository.saveAll(pastAppointments) }
    }
}

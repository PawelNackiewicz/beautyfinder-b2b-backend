package com.beautyfinder.b2b.application.employee

import com.beautyfinder.b2b.domain.User
import com.beautyfinder.b2b.domain.UserRole
import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.domain.appointment.AppointmentSource
import com.beautyfinder.b2b.domain.appointment.AppointmentStatus
import com.beautyfinder.b2b.domain.appointment.TimeSlot
import com.beautyfinder.b2b.domain.employee.CannotDeleteActiveEmployeeException
import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.employee.EmployeeNotFoundException
import com.beautyfinder.b2b.domain.employee.EmployeeStatus
import com.beautyfinder.b2b.domain.employee.InvalidScheduleException
import com.beautyfinder.b2b.domain.employee.ScheduleExceptionOverlapException
import com.beautyfinder.b2b.domain.schedule.ScheduleException
import com.beautyfinder.b2b.domain.schedule.ScheduleExceptionType
import com.beautyfinder.b2b.domain.schedule.WeeklySchedule
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.ScheduleExceptionRepository
import com.beautyfinder.b2b.infrastructure.UserRepository
import com.beautyfinder.b2b.infrastructure.WeeklyScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class EmployeeServiceTest {

    private val employeeRepository = mockk<EmployeeRepository>(relaxed = true)
    private val weeklyScheduleRepository = mockk<WeeklyScheduleRepository>(relaxed = true)
    private val scheduleExceptionRepository = mockk<ScheduleExceptionRepository>(relaxed = true)
    private val appointmentRepository = mockk<AppointmentRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val employeeMapper = mockk<EmployeeMapper>()

    private val service = EmployeeService(
        employeeRepository = employeeRepository,
        weeklyScheduleRepository = weeklyScheduleRepository,
        scheduleExceptionRepository = scheduleExceptionRepository,
        appointmentRepository = appointmentRepository,
        userRepository = userRepository,
        employeeMapper = employeeMapper,
    )

    private val salonId = UUID.randomUUID()
    private val employeeId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    // --- Helpers ---

    private fun setId(entity: com.beautyfinder.b2b.domain.BaseEntity, id: UUID) {
        val field = com.beautyfinder.b2b.domain.BaseEntity::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(entity, id)
    }

    private fun buildEmployee(
        id: UUID = employeeId,
        status: EmployeeStatus = EmployeeStatus.ACTIVE,
    ): Employee {
        val emp = Employee(
            salonId = salonId,
            userId = userId,
            displayName = "John",
            status = status,
        )
        setId(emp, id)
        return emp
    }

    private fun buildEmployeeDto(
        id: UUID = employeeId,
        status: EmployeeStatus = EmployeeStatus.ACTIVE,
    ) = EmployeeDto(
        id = id,
        salonId = salonId,
        userId = userId,
        displayName = "John",
        phone = null,
        avatarUrl = null,
        color = null,
        status = status,
        serviceIds = emptySet(),
        createdAt = null,
        updatedAt = null,
    )

    private fun buildWeeklySchedule(
        day: DayOfWeek = DayOfWeek.MONDAY,
        startTime: LocalTime = LocalTime.of(9, 0),
        endTime: LocalTime = LocalTime.of(17, 0),
        isWorkingDay: Boolean = true,
    ): WeeklySchedule {
        val ws = WeeklySchedule(
            employeeId = employeeId,
            salonId = salonId,
            dayOfWeek = day,
            startTime = startTime,
            endTime = endTime,
            isWorkingDay = isWorkingDay,
        )
        setId(ws, UUID.randomUUID())
        return ws
    }

    private fun buildAppointment(
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    ): Appointment {
        val appt = Appointment(
            salonId = salonId,
            clientId = UUID.randomUUID(),
            employeeId = employeeId,
            variantId = UUID.randomUUID(),
            startAt = startAt,
            endAt = endAt,
            status = status,
            source = AppointmentSource.DIRECT,
        )
        setId(appt, UUID.randomUUID())
        return appt
    }

    private fun buildScheduleException(
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): ScheduleException {
        val ex = ScheduleException(
            employeeId = employeeId,
            salonId = salonId,
            startAt = startAt,
            endAt = endAt,
            reason = "Test",
            type = ScheduleExceptionType.BLOCKED,
        )
        setId(ex, UUID.randomUUID())
        return ex
    }

    // --- Tests ---

    @Test
    fun `listEmployees - only active filters inactive`() {
        // given
        val activeEmp = buildEmployee()
        every { employeeRepository.findAllBySalonIdAndStatusOrderByDisplayNameAsc(salonId, EmployeeStatus.ACTIVE) } returns listOf(activeEmp)
        every { employeeMapper.toDtoList(listOf(activeEmp)) } returns listOf(buildEmployeeDto())

        // when
        val result = service.listEmployees(salonId, includeInactive = false)

        // then
        assertEquals(1, result.size)
        verify { employeeRepository.findAllBySalonIdAndStatusOrderByDisplayNameAsc(salonId, EmployeeStatus.ACTIVE) }
    }

    @Test
    fun `listEmployees - includeInactive returns all non-deleted`() {
        // given
        val employees = listOf(buildEmployee(), buildEmployee(id = UUID.randomUUID(), status = EmployeeStatus.INACTIVE))
        every { employeeRepository.findAllBySalonIdAndStatusNotOrderByDisplayNameAsc(salonId, EmployeeStatus.DELETED) } returns employees
        every { employeeMapper.toDtoList(employees) } returns employees.map { buildEmployeeDto(it.id!!, it.status) }

        // when
        val result = service.listEmployees(salonId, includeInactive = true)

        // then
        assertEquals(2, result.size)
    }

    @Test
    fun `createEmployee - success sets ACTIVE status`() {
        // given
        val user = User(salonId = salonId, role = UserRole.EMPLOYEE, email = "test@test.com", passwordHash = "hash")
        setId(user, userId)
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { employeeRepository.existsByUserIdAndSalonIdAndStatusNot(userId, salonId, EmployeeStatus.DELETED) } returns false
        every { employeeRepository.save(any()) } answers { firstArg() }
        every { employeeMapper.toDto(any()) } returns buildEmployeeDto()

        val request = CreateEmployeeRequest(userId = userId, displayName = "John")

        // when
        val result = service.createEmployee(request, salonId)

        // then
        assertEquals(EmployeeStatus.ACTIVE, result.status)
    }

    @Test
    fun `createEmployee - duplicate user throws exception`() {
        // given
        val user = User(salonId = salonId, role = UserRole.EMPLOYEE, email = "test@test.com", passwordHash = "hash")
        setId(user, userId)
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { employeeRepository.existsByUserIdAndSalonIdAndStatusNot(userId, salonId, EmployeeStatus.DELETED) } returns true

        val request = CreateEmployeeRequest(userId = userId, displayName = "John")

        // when/then
        assertThrows<IllegalArgumentException> {
            service.createEmployee(request, salonId)
        }
    }

    @Test
    fun `updateEmployee - changes displayName`() {
        // given
        val employee = buildEmployee()
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { employeeRepository.save(any()) } answers { firstArg() }
        every { employeeMapper.toDto(any()) } answers {
            val e = firstArg<Employee>()
            buildEmployeeDto().copy(displayName = e.displayName)
        }

        val request = UpdateEmployeeRequest(displayName = "Jane")

        // when
        val result = service.updateEmployee(employeeId, request, salonId)

        // then
        assertEquals("Jane", result.displayName)
    }

    @Test
    fun `deactivateEmployee - has future appointments throws exception`() {
        // given
        val employee = buildEmployee()
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { appointmentRepository.findConflictingAppointments(eq(employeeId), any(), any(), any<List<AppointmentStatus>>()) } returns
            listOf(buildAppointment(OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1)))

        // when/then
        assertThrows<CannotDeleteActiveEmployeeException> {
            service.deactivateEmployee(employeeId, salonId)
        }
    }

    @Test
    fun `deactivateEmployee - no future appointments sets INACTIVE`() {
        // given
        val employee = buildEmployee()
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { appointmentRepository.findConflictingAppointments(eq(employeeId), any(), any(), any<List<AppointmentStatus>>()) } returns emptyList()
        every { employeeRepository.save(any()) } answers { firstArg() }

        // when
        service.deactivateEmployee(employeeId, salonId)

        // then
        assertEquals(EmployeeStatus.INACTIVE, employee.status)
        verify { employeeRepository.save(employee) }
    }

    @Test
    fun `upsertWeeklySchedule - replaces existing`() {
        // given
        val employee = buildEmployee()
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { weeklyScheduleRepository.deleteAllByEmployeeId(employeeId) } returns Unit
        every { weeklyScheduleRepository.saveAll(any<List<WeeklySchedule>>()) } answers { firstArg() }
        every { employeeMapper.toScheduleDto(any()) } answers {
            val s = firstArg<WeeklySchedule>()
            WeeklyScheduleDto(s.id, s.employeeId, s.dayOfWeek, s.startTime, s.endTime, s.isWorkingDay)
        }

        val schedules = listOf(
            UpsertScheduleRequest(DayOfWeek.MONDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
            UpsertScheduleRequest(DayOfWeek.TUESDAY, true, LocalTime.of(9, 0), LocalTime.of(17, 0)),
        )

        // when
        val result = service.upsertWeeklySchedule(employeeId, schedules, salonId)

        // then
        verify { weeklyScheduleRepository.deleteAllByEmployeeId(employeeId) }
        verify { weeklyScheduleRepository.saveAll(any<List<WeeklySchedule>>()) }
        assertEquals(7, result.size) // always returns 7 days
    }

    @Test
    fun `upsertWeeklySchedule - invalid hours throws InvalidScheduleException`() {
        // given
        val employee = buildEmployee()
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee

        val schedules = listOf(
            UpsertScheduleRequest(DayOfWeek.MONDAY, true, LocalTime.of(17, 0), LocalTime.of(9, 0)),
        )

        // when/then
        assertThrows<InvalidScheduleException> {
            service.upsertWeeklySchedule(employeeId, schedules, salonId)
        }
    }

    @Test
    fun `addScheduleException - no overlap saves exception`() {
        // given
        val employee = buildEmployee()
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(any(), any(), any()) } returns emptyList()
        every { scheduleExceptionRepository.save(any()) } answers {
            val ex = firstArg<ScheduleException>()
            setId(ex, UUID.randomUUID())
            ex
        }
        every { employeeMapper.toExceptionDto(any()) } answers {
            val ex = firstArg<ScheduleException>()
            ScheduleExceptionDto(ex.id!!, ex.employeeId, ex.startAt, ex.endAt, ex.reason, ex.type, ex.createdAt)
        }

        val request = AddExceptionRequest(
            startAt = OffsetDateTime.now().plusDays(5),
            endAt = OffsetDateTime.now().plusDays(6),
            reason = "Vacation",
            type = ScheduleExceptionType.VACATION,
        )

        // when
        val result = service.addScheduleException(employeeId, request, salonId)

        // then
        verify { scheduleExceptionRepository.save(any()) }
        assertEquals(ScheduleExceptionType.VACATION, result.type)
    }

    @Test
    fun `addScheduleException - overlaps existing throws ScheduleExceptionOverlapException`() {
        // given
        val employee = buildEmployee()
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(any(), any(), any()) } returns
            listOf(buildScheduleException(OffsetDateTime.now().plusDays(4), OffsetDateTime.now().plusDays(7)))

        val request = AddExceptionRequest(
            startAt = OffsetDateTime.now().plusDays(5),
            endAt = OffsetDateTime.now().plusDays(6),
            reason = "Overlap",
            type = ScheduleExceptionType.BLOCKED,
        )

        // when/then
        assertThrows<ScheduleExceptionOverlapException> {
            service.addScheduleException(employeeId, request, salonId)
        }
    }

    @Test
    fun `getAvailableSlots - no schedule returns empty`() {
        // given
        val employee = buildEmployee()
        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, any()) } returns null

        // when
        val result = service.getAvailableSlots(employeeId, LocalDate.of(2099, 6, 16), 60, salonId) // Monday

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAvailableSlots - fully booked returns empty`() {
        // given
        val employee = buildEmployee()
        val date = LocalDate.of(2099, 6, 16) // Monday
        val workStart = date.atTime(9, 0).atOffset(ZoneOffset.UTC)
        val workEnd = date.atTime(10, 0).atOffset(ZoneOffset.UTC)

        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, DayOfWeek.MONDAY) } returns
            buildWeeklySchedule(day = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(any(), any(), any()) } returns emptyList()
        every { appointmentRepository.findConflictingAppointments(eq(employeeId), any(), any(), any<List<AppointmentStatus>>()) } returns
            listOf(buildAppointment(workStart, workEnd))

        // when
        val result = service.getAvailableSlots(employeeId, date, 60, salonId)

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAvailableSlots - partially booked returns only free slots`() {
        // given
        val employee = buildEmployee()
        // 2099-06-16 is a Wednesday, not Monday - fix the day
        val date = LocalDate.of(2099, 6, 16)
        val dayOfWeek = date.dayOfWeek // WEDNESDAY

        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek) } returns
            buildWeeklySchedule(day = dayOfWeek, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(12, 0))
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(any(), any(), any()) } returns emptyList()

        // Booked: 10:00-11:00
        val bookedStart = date.atTime(10, 0).atOffset(ZoneOffset.UTC)
        val bookedEnd = date.atTime(11, 0).atOffset(ZoneOffset.UTC)
        every { appointmentRepository.findConflictingAppointments(eq(employeeId), any(), any(), any<List<AppointmentStatus>>()) } returns
            listOf(buildAppointment(bookedStart, bookedEnd))

        // when - looking for 60 min slots
        val result = service.getAvailableSlots(employeeId, date, 60, salonId)

        // then - 9:00-10:00 and 11:00-12:00 should be available
        assertTrue(result.size >= 2, "Expected at least 2 slots but got ${result.size}: ${result.map { it.start.toLocalTime() }}")
        assertTrue(result.any { it.start.toLocalTime() == LocalTime.of(9, 0) })
        assertTrue(result.any { it.start.toLocalTime() == LocalTime.of(11, 0) })
        assertTrue(result.none { it.start.toLocalTime() == LocalTime.of(10, 0) })
    }

    @Test
    fun `getAvailableSlots - schedule exception blocks day`() {
        // given
        val employee = buildEmployee()
        val date = LocalDate.of(2099, 6, 16) // Monday

        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, DayOfWeek.MONDAY) } returns
            buildWeeklySchedule(day = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(17, 0))

        // Full-day block
        val blockStart = date.atStartOfDay().atOffset(ZoneOffset.UTC)
        val blockEnd = date.atTime(23, 59).atOffset(ZoneOffset.UTC)
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(any(), any(), any()) } returns
            listOf(buildScheduleException(blockStart, blockEnd))

        // when
        val result = service.getAvailableSlots(employeeId, date, 60, salonId)

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAvailableSlots - past slots are filtered`() {
        // given
        val employee = buildEmployee()
        // Use a date in far future to avoid all being filtered
        val date = LocalDate.of(2099, 6, 16)

        every { employeeRepository.findByIdAndSalonId(employeeId, salonId) } returns employee
        every { weeklyScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, DayOfWeek.MONDAY) } returns
            buildWeeklySchedule(day = DayOfWeek.MONDAY, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(17, 0))
        every { scheduleExceptionRepository.findByEmployeeIdAndStartAtLessThanAndEndAtGreaterThan(any(), any(), any()) } returns emptyList()
        every { appointmentRepository.findConflictingAppointments(eq(employeeId), any(), any(), any<List<AppointmentStatus>>()) } returns emptyList()

        // when
        val result = service.getAvailableSlots(employeeId, date, 60, salonId)

        // then - all slots should be in the future
        assertTrue(result.all { it.start.isAfter(OffsetDateTime.now()) })
    }
}

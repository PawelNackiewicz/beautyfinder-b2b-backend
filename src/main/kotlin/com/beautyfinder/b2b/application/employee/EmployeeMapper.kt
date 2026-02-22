package com.beautyfinder.b2b.application.employee

import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.schedule.ScheduleException
import com.beautyfinder.b2b.domain.schedule.WeeklySchedule
import org.springframework.stereotype.Component

interface EmployeeMapper {
    fun toDto(employee: Employee): EmployeeDto
    fun toDtoList(employees: List<Employee>): List<EmployeeDto>
    fun toScheduleDto(schedule: WeeklySchedule): WeeklyScheduleDto
    fun toScheduleDtoList(schedules: List<WeeklySchedule>): List<WeeklyScheduleDto>
    fun toExceptionDto(exception: ScheduleException): ScheduleExceptionDto
    fun toExceptionDtoList(exceptions: List<ScheduleException>): List<ScheduleExceptionDto>
}

@Component
class EmployeeMapperImpl : EmployeeMapper {

    override fun toDto(employee: Employee): EmployeeDto = EmployeeDto(
        id = employee.id!!,
        salonId = employee.salonId,
        userId = employee.userId,
        displayName = employee.displayName,
        phone = employee.phone,
        avatarUrl = employee.avatarUrl,
        color = employee.color,
        status = employee.status,
        serviceIds = employee.serviceIds.toSet(),
        createdAt = employee.createdAt,
        updatedAt = employee.updatedAt,
    )

    override fun toDtoList(employees: List<Employee>): List<EmployeeDto> =
        employees.map { toDto(it) }

    override fun toScheduleDto(schedule: WeeklySchedule): WeeklyScheduleDto = WeeklyScheduleDto(
        id = schedule.id,
        employeeId = schedule.employeeId,
        dayOfWeek = schedule.dayOfWeek,
        startTime = schedule.startTime,
        endTime = schedule.endTime,
        isWorkingDay = schedule.isWorkingDay,
    )

    override fun toScheduleDtoList(schedules: List<WeeklySchedule>): List<WeeklyScheduleDto> =
        schedules.map { toScheduleDto(it) }

    override fun toExceptionDto(exception: ScheduleException): ScheduleExceptionDto = ScheduleExceptionDto(
        id = exception.id!!,
        employeeId = exception.employeeId,
        startAt = exception.startAt,
        endAt = exception.endAt,
        reason = exception.reason,
        type = exception.type,
        createdAt = exception.createdAt,
    )

    override fun toExceptionDtoList(exceptions: List<ScheduleException>): List<ScheduleExceptionDto> =
        exceptions.map { toExceptionDto(it) }
}

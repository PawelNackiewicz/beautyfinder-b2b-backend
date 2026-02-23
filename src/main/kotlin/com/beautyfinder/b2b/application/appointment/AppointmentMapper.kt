package com.beautyfinder.b2b.application.appointment

import com.beautyfinder.b2b.domain.appointment.Appointment
import com.beautyfinder.b2b.infrastructure.ClientRepository
import com.beautyfinder.b2b.infrastructure.EmployeeRepository
import com.beautyfinder.b2b.infrastructure.ServiceRepository
import com.beautyfinder.b2b.infrastructure.ServiceVariantRepository
import org.springframework.stereotype.Component

interface AppointmentMapper {
    fun toDto(appointment: Appointment): AppointmentDto
    fun toDtoList(appointments: List<Appointment>): List<AppointmentDto>
}

@Component
class AppointmentMapperImpl(
    private val employeeRepository: EmployeeRepository,
    private val serviceVariantRepository: ServiceVariantRepository,
    private val serviceRepository: ServiceRepository,
    private val clientRepository: ClientRepository,
) : AppointmentMapper {

    override fun toDto(appointment: Appointment): AppointmentDto {
        val employee = employeeRepository.findById(appointment.employeeId).orElse(null)
        val variant = serviceVariantRepository.findById(appointment.variantId).orElse(null)
        val service = variant?.let { serviceRepository.findById(it.serviceId).orElse(null) }
        val client = clientRepository.findById(appointment.clientId).orElse(null)

        return AppointmentDto(
            id = appointment.id!!,
            salonId = appointment.salonId,
            clientId = appointment.clientId,
            clientName = client?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown",
            employeeId = appointment.employeeId,
            employeeName = employee?.displayName ?: "Unknown",
            variantId = appointment.variantId,
            variantName = variant?.name ?: "Unknown",
            serviceName = service?.name ?: "Unknown",
            startAt = appointment.startAt,
            endAt = appointment.endAt,
            status = appointment.status,
            source = appointment.source,
            finalPrice = appointment.finalPrice,
            commissionValue = appointment.commissionValue,
            notes = appointment.notes,
            cancellationReason = appointment.cancellationReason,
            createdAt = appointment.createdAt,
            updatedAt = appointment.updatedAt,
        )
    }

    override fun toDtoList(appointments: List<Appointment>): List<AppointmentDto> =
        appointments.map { toDto(it) }
}

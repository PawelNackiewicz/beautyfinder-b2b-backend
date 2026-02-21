package com.beautyfinder.b2b.application

import com.beautyfinder.b2b.api.AppointmentResponse
import com.beautyfinder.b2b.api.CreateAppointmentRequest
import com.beautyfinder.b2b.domain.Appointment
import com.beautyfinder.b2b.domain.AppointmentStatus
import com.beautyfinder.b2b.infrastructure.AppointmentRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
) {

    fun listAppointments(salonId: UUID): List<AppointmentResponse> =
        appointmentRepository.findAllBySalonId(salonId).map { it.toResponse() }

    fun createAppointment(request: CreateAppointmentRequest, salonId: UUID): AppointmentResponse {
        val appointment = Appointment(
            salonId = salonId,
            clientId = request.clientId,
            employeeId = request.employeeId,
            variantId = request.variantId,
            startAt = request.startAt,
            status = AppointmentStatus.SCHEDULED,
        )
        return appointmentRepository.save(appointment).toResponse()
    }

    private fun Appointment.toResponse() = AppointmentResponse(
        id = id!!,
        salonId = salonId,
        clientId = clientId,
        employeeId = employeeId,
        variantId = variantId,
        startAt = startAt,
        status = status.name,
        finalPrice = finalPrice,
        source = source,
        createdAt = createdAt,
    )
}

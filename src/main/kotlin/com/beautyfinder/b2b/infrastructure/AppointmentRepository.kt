package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.Appointment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AppointmentRepository : JpaRepository<Appointment, UUID> {
    fun findAllBySalonId(salonId: UUID): List<Appointment>
}

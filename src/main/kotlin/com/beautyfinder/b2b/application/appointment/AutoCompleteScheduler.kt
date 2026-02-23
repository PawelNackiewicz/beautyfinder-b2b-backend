package com.beautyfinder.b2b.application.appointment

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class AutoCompleteScheduler(
    private val appointmentService: AppointmentService,
) {

    private val log = LoggerFactory.getLogger(AutoCompleteScheduler::class.java)

    @Scheduled(cron = "0 0 23 * * *")
    fun autoComplete() {
        val count = appointmentService.autoCompleteAppointments(OffsetDateTime.now())
        log.info("Auto-completed {} appointments", count)
    }
}

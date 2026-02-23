package com.beautyfinder.b2b.domain.appointment

object AppointmentStatusMachine {

    private val allowedTransitions: Map<AppointmentStatus, Set<AppointmentStatus>> = mapOf(
        AppointmentStatus.SCHEDULED to setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED),
        AppointmentStatus.CONFIRMED to setOf(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
        AppointmentStatus.IN_PROGRESS to setOf(AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW),
        AppointmentStatus.COMPLETED to emptySet(),
        AppointmentStatus.NO_SHOW to emptySet(),
        AppointmentStatus.CANCELLED to emptySet(),
    )

    fun validateTransition(from: AppointmentStatus, to: AppointmentStatus): Result<Unit> {
        val allowed = allowedTransitions[from] ?: emptySet()
        return if (to in allowed) {
            Result.success(Unit)
        } else {
            Result.failure(InvalidStatusTransitionException(from, to))
        }
    }
}

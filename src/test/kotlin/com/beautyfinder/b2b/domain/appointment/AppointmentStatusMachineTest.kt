package com.beautyfinder.b2b.domain.appointment

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class AppointmentStatusMachineTest {

    companion object {
        @JvmStatic
        fun allowedTransitions(): Stream<Pair<AppointmentStatus, AppointmentStatus>> = Stream.of(
            AppointmentStatus.SCHEDULED to AppointmentStatus.CONFIRMED,
            AppointmentStatus.SCHEDULED to AppointmentStatus.CANCELLED,
            AppointmentStatus.CONFIRMED to AppointmentStatus.IN_PROGRESS,
            AppointmentStatus.CONFIRMED to AppointmentStatus.CANCELLED,
            AppointmentStatus.CONFIRMED to AppointmentStatus.NO_SHOW,
            AppointmentStatus.IN_PROGRESS to AppointmentStatus.COMPLETED,
            AppointmentStatus.IN_PROGRESS to AppointmentStatus.NO_SHOW,
        )

        @JvmStatic
        fun disallowedTransitions(): Stream<Pair<AppointmentStatus, AppointmentStatus>> = Stream.of(
            AppointmentStatus.COMPLETED to AppointmentStatus.SCHEDULED,
            AppointmentStatus.COMPLETED to AppointmentStatus.CANCELLED,
            AppointmentStatus.CANCELLED to AppointmentStatus.CONFIRMED,
            AppointmentStatus.CANCELLED to AppointmentStatus.SCHEDULED,
            AppointmentStatus.NO_SHOW to AppointmentStatus.COMPLETED,
            AppointmentStatus.NO_SHOW to AppointmentStatus.SCHEDULED,
            AppointmentStatus.SCHEDULED to AppointmentStatus.COMPLETED,
            AppointmentStatus.SCHEDULED to AppointmentStatus.IN_PROGRESS,
        )
    }

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    fun `allowed transition should succeed`(transition: Pair<AppointmentStatus, AppointmentStatus>) {
        // given
        val (from, to) = transition

        // when
        val result = AppointmentStatusMachine.validateTransition(from, to)

        // then
        assertTrue(result.isSuccess)
    }

    @ParameterizedTest
    @MethodSource("disallowedTransitions")
    fun `disallowed transition should throw InvalidStatusTransitionException`(transition: Pair<AppointmentStatus, AppointmentStatus>) {
        // given
        val (from, to) = transition

        // when/then
        val result = AppointmentStatusMachine.validateTransition(from, to)
        assertTrue(result.isFailure)
        assertThrows<InvalidStatusTransitionException> { result.getOrThrow() }
    }
}

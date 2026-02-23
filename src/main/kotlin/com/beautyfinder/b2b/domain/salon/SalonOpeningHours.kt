package com.beautyfinder.b2b.domain.salon

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(
    name = "salon_opening_hours",
    uniqueConstraints = [UniqueConstraint(columnNames = ["salon_id", "day_of_week"])],
)
class SalonOpeningHours(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: DayOfWeek,

    @Column(name = "is_open", nullable = false)
    val isOpen: Boolean,

    @Column(name = "open_time")
    val openTime: LocalTime? = null,

    @Column(name = "close_time")
    val closeTime: LocalTime? = null,

    @Column(name = "break_start")
    val breakStart: LocalTime? = null,

    @Column(name = "break_end")
    val breakEnd: LocalTime? = null,
) : BaseEntity()

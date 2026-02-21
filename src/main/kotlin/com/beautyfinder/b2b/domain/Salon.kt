package com.beautyfinder.b2b.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "salons")
class Salon(
    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    var slug: String,

    @Column(name = "cancellation_window_hours", nullable = false)
    var cancellationWindowHours: Int = 24,
) : BaseEntity()

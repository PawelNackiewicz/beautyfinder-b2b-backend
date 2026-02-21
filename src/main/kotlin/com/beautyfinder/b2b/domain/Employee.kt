package com.beautyfinder.b2b.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "employees")
class Employee(
    @Column(name = "salon_id", nullable = false)
    var salonId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "display_name", nullable = false)
    var displayName: String,
) : BaseEntity()

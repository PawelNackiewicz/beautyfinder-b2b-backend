package com.beautyfinder.b2b.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "clients")
class Client(
    @Column(name = "salon_id", nullable = false)
    var salonId: UUID,

    @Column(nullable = false)
    var name: String,

    var phone: String? = null,

    var email: String? = null,
) : BaseEntity()

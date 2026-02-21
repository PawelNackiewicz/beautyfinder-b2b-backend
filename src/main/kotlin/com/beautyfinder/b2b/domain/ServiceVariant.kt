package com.beautyfinder.b2b.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "service_variants")
class ServiceVariant(
    @Column(name = "service_id", nullable = false)
    var serviceId: UUID,

    @Column(nullable = false)
    var name: String,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int,

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,
) : BaseEntity()

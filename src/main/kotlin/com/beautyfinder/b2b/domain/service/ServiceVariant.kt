package com.beautyfinder.b2b.domain.service

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(
    name = "service_variants",
    indexes = [
        Index(name = "idx_variants_service", columnList = "service_id"),
    ],
)
class ServiceVariant(
    @Column(name = "service_id", nullable = false)
    val serviceId: UUID,

    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int,

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,

    @Column(name = "price_max", precision = 10, scale = 2)
    var priceMax: BigDecimal? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VariantStatus = VariantStatus.ACTIVE,

    @Column(name = "is_online_bookable", nullable = false)
    var isOnlineBookable: Boolean = true,
) : BaseEntity()

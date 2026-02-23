package com.beautyfinder.b2b.domain.service

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "service_categories",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["salon_id", "name"]),
    ],
)
class ServiceCategory(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "color_hex", length = 7)
    var colorHex: String? = null,

    @Column(name = "icon_name", length = 50)
    var iconName: String? = null,
) : BaseEntity()

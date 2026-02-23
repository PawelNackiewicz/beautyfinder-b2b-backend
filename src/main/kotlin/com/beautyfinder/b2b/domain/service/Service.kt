package com.beautyfinder.b2b.domain.service

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "services",
    indexes = [
        Index(name = "idx_services_salon", columnList = "salon_id"),
        Index(name = "idx_services_category", columnList = "salon_id,category"),
    ],
)
class Service(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 50)
    var category: String,

    @Column(length = 1000)
    var description: String? = null,

    @Column(name = "image_url")
    var imageUrl: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ServiceStatus = ServiceStatus.ACTIVE,

    @Column(name = "is_online_bookable", nullable = false)
    var isOnlineBookable: Boolean = true,

    @Column(name = "category_id")
    var categoryId: UUID? = null,
) : BaseEntity()

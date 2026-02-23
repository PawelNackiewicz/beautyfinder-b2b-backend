package com.beautyfinder.b2b.domain.employee

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.util.UUID

enum class EmployeeStatus {
    ACTIVE, INACTIVE, DELETED
}

@Entity
@Table(name = "employees")
class Employee(
    @Column(name = "salon_id", nullable = false)
    var salonId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    var phone: String? = null,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EmployeeStatus = EmployeeStatus.ACTIVE,

    @ElementCollection
    @CollectionTable(name = "employee_services", joinColumns = [JoinColumn(name = "employee_id")])
    @Column(name = "service_id")
    var serviceIds: MutableSet<UUID> = mutableSetOf(),

    var color: String? = null,
) : BaseEntity()

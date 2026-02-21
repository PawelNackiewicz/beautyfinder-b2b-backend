package com.beautyfinder.b2b.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

enum class UserRole {
    OWNER, MANAGER, EMPLOYEE
}

@Entity
@Table(name = "users")
class User(
    @Column(name = "salon_id", nullable = false)
    var salonId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
) : BaseEntity()

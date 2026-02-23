package com.beautyfinder.b2b.domain.client

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class ClientStatus { ACTIVE, BLOCKED, DELETED }

@Entity
@Table(
    name = "clients",
    indexes = [
        Index(name = "idx_clients_salon", columnList = "salon_id"),
        Index(name = "idx_clients_phone", columnList = "salon_id,phone"),
        Index(name = "idx_clients_email", columnList = "salon_id,email"),
    ],
)
class Client(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

    var phone: String? = null,

    var email: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ClientStatus = ClientStatus.ACTIVE,

    var source: String? = null,

    @Column(name = "external_id")
    var externalId: String? = null,

    @Column(name = "preferred_employee_id")
    var preferredEmployeeId: UUID? = null,

    @Column(length = 2000)
    var notes: String? = null,

    @Column(name = "total_visits", nullable = false)
    var totalVisits: Int = 0,

    @Column(name = "total_spent", nullable = false)
    var totalSpent: BigDecimal = BigDecimal.ZERO,

    @Column(name = "last_visit_at")
    var lastVisitAt: OffsetDateTime? = null,
) : BaseEntity()

fun String.normalizePhone(): String = this.replace(Regex("[\\s\\-]"), "")

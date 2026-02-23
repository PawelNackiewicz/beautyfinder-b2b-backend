package com.beautyfinder.b2b.domain.client

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "blacklist_entries")
class BlacklistEntry(
    @Column(name = "client_id", nullable = false)
    val clientId: UUID,

    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(nullable = false)
    var reason: String,

    @Column(name = "created_by", nullable = false)
    var createdBy: UUID,

    @Column(name = "removed_at")
    var removedAt: OffsetDateTime? = null,

    @Column(name = "removed_by")
    var removedBy: UUID? = null,
) : BaseEntity() {
    val isActive: Boolean get() = removedAt == null
}

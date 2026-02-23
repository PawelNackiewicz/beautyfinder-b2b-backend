package com.beautyfinder.b2b.domain.audit

import com.beautyfinder.b2b.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "audit_retention_policies",
    uniqueConstraints = [UniqueConstraint(columnNames = ["salon_id"])],
)
class AuditLogRetentionPolicy(
    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(name = "retention_days", nullable = false)
    var retentionDays: Int = 365,

    @Column(name = "sensitive_data_retention_days", nullable = false)
    var sensitiveDataRetentionDays: Int = 1825,
) : BaseEntity()

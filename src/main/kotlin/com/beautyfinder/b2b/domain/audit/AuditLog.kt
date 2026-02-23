package com.beautyfinder.b2b.domain.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "audit_logs",
    indexes = [
        Index(name = "idx_audit_salon_time", columnList = "salon_id,created_at"),
        Index(name = "idx_audit_resource", columnList = "resource_type,resource_id"),
        Index(name = "idx_audit_actor", columnList = "actor_id"),
        Index(name = "idx_audit_action", columnList = "action"),
    ],
)
class AuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "salon_id", nullable = false)
    val salonId: UUID,

    @Column(name = "actor_id")
    val actorId: UUID?,

    @Column(name = "actor_email")
    val actorEmail: String?,

    @Column(name = "actor_role")
    val actorRole: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val action: AuditAction,

    @Column(name = "resource_type", nullable = false)
    val resourceType: String,

    @Column(name = "resource_id")
    val resourceId: UUID?,

    @Column(name = "resource_description")
    val resourceDescription: String?,

    @Column(columnDefinition = "jsonb")
    val metadata: String?,

    @Column(name = "changed_fields", columnDefinition = "jsonb")
    val changedFields: String?,

    @Column(name = "ip_address")
    val ipAddress: String?,

    @Column(name = "user_agent")
    val userAgent: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)

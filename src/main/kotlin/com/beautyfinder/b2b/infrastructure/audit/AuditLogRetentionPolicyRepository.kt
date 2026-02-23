package com.beautyfinder.b2b.infrastructure.audit

import com.beautyfinder.b2b.domain.audit.AuditLogRetentionPolicy
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuditLogRetentionPolicyRepository : JpaRepository<AuditLogRetentionPolicy, UUID> {

    fun findBySalonId(salonId: UUID): AuditLogRetentionPolicy?
}

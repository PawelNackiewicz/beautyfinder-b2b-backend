package com.beautyfinder.b2b.domain.audit

import java.util.UUID

data class AuditContext(
    val salonId: UUID,
    val actorId: UUID?,
    val actorEmail: String?,
    val actorRole: String?,
    val ipAddress: String?,
    val userAgent: String?,
)

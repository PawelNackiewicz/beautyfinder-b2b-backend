package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditAction
import com.beautyfinder.b2b.domain.audit.ChangedFields
import java.time.OffsetDateTime
import java.util.UUID

data class AuditLogEntry(
    val salonId: UUID,
    val action: AuditAction,
    val resourceType: String,
    val resourceId: UUID? = null,
    val resourceDescription: String? = null,
    val metadata: Map<String, Any?>? = null,
    val changedFields: ChangedFields? = null,
    val context: com.beautyfinder.b2b.domain.audit.AuditContext? = null,
)

data class AuditLogDto(
    val id: UUID,
    val salonId: UUID,
    val actorId: UUID?,
    val actorEmail: String?,
    val actorRole: String?,
    val action: AuditAction,
    val resourceType: String,
    val resourceId: UUID?,
    val resourceDescription: String?,
    val metadata: Map<String, Any?>?,
    val changedFields: List<FieldChangeDto>?,
    val ipAddress: String?,
    val createdAt: OffsetDateTime,
)

data class FieldChangeDto(val field: String, val oldValue: Any?, val newValue: Any?)

data class AuditSummaryDto(
    val totalActions: Long,
    val actionBreakdown: Map<String, Long>,
    val mostActiveActors: List<ActorActivityDto>,
    val sensitiveDataAccesses: Long,
    val period: String,
)

data class ActorActivityDto(val actorId: UUID, val actorEmail: String?, val actionCount: Long)

data class AuditSearchQuery(
    val actorId: UUID? = null,
    val action: AuditAction? = null,
    val resourceType: String? = null,
    val resourceId: UUID? = null,
    val from: OffsetDateTime? = null,
    val to: OffsetDateTime? = null,
    val ipAddress: String? = null,
)

enum class StatsPeriod(val days: Long) {
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
    LAST_90_DAYS(90),
    LAST_YEAR(365),
}

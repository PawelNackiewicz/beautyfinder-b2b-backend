package com.beautyfinder.b2b.application.audit

import com.beautyfinder.b2b.domain.audit.AuditAction
import com.beautyfinder.b2b.domain.audit.AuditLog
import com.beautyfinder.b2b.infrastructure.audit.AuditLogRepository
import com.beautyfinder.b2b.infrastructure.audit.AuditLogRetentionPolicyRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
    private val retentionPolicyRepository: AuditLogRetentionPolicyRepository,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(AuditLogService::class.java)

    companion object {
        private const val DEFAULT_RETENTION_DAYS = 365L
        private const val DEFAULT_SENSITIVE_RETENTION_DAYS = 1825L
        private val SENSITIVE_ACTIONS = listOf(
            AuditAction.CLIENT_SENSITIVE_DATA_ACCESSED,
            AuditAction.CLIENT_SENSITIVE_DATA_UPDATED,
        )
    }

    @Async("auditExecutor")
    fun log(entry: AuditLogEntry) {
        try {
            val ctx = entry.context ?: AuditContextHolder.get()

            val auditLog = AuditLog(
                salonId = entry.salonId,
                actorId = ctx?.actorId,
                actorEmail = ctx?.actorEmail,
                actorRole = ctx?.actorRole,
                action = entry.action,
                resourceType = entry.resourceType,
                resourceId = entry.resourceId,
                resourceDescription = entry.resourceDescription,
                metadata = entry.metadata?.let { objectMapper.writeValueAsString(it) },
                changedFields = entry.changedFields?.toJson(),
                ipAddress = ctx?.ipAddress,
                userAgent = ctx?.userAgent,
            )
            auditLogRepository.save(auditLog)
        } catch (e: Exception) {
            log.error("Failed to save audit log entry: action={}, resourceType={}", entry.action, entry.resourceType, e)
        }
    }

    fun searchLogs(query: AuditSearchQuery, salonId: UUID, pageable: Pageable): Page<AuditLogDto> {
        return auditLogRepository.search(
            salonId = salonId,
            actorId = query.actorId,
            action = query.action,
            resourceType = query.resourceType,
            resourceId = query.resourceId,
            from = query.from,
            to = query.to,
            pageable = pageable,
        ).map { it.toDto() }
    }

    fun getLogsForResource(resourceType: String, resourceId: UUID, salonId: UUID): List<AuditLogDto> {
        return auditLogRepository.findAllByResourceTypeAndResourceIdAndSalonIdOrderByCreatedAtDesc(
            resourceType, resourceId, salonId, PageRequest.of(0, 200),
        ).content.map { it.toDto() }
    }

    fun getLogsForActor(actorId: UUID, salonId: UUID, pageable: Pageable): Page<AuditLogDto> {
        return auditLogRepository.findAllByActorIdAndSalonIdOrderByCreatedAtDesc(
            actorId, salonId, pageable,
        ).map { it.toDto() }
    }

    fun getSensitiveDataAccessLog(clientId: UUID, salonId: UUID): List<AuditLogDto> {
        return auditLogRepository.findAllByResourceTypeAndResourceIdAndSalonIdAndActionIn(
            "CLIENT", clientId, salonId, SENSITIVE_ACTIONS,
        ).map { it.toDto() }
    }

    fun getSummary(salonId: UUID, period: StatsPeriod): AuditSummaryDto {
        val from = OffsetDateTime.now().minusDays(period.days)

        val actionBreakdown = auditLogRepository.countByActionGrouped(salonId, from)
            .associate { it.action.name to it.cnt }

        val totalActions = actionBreakdown.values.sum()

        val mostActiveActors = auditLogRepository.findMostActiveActors(salonId, from, PageRequest.of(0, 10))
            .map { ActorActivityDto(it.actorId, it.actorEmail, it.actionCount) }

        val sensitiveDataAccesses = SENSITIVE_ACTIONS.sumOf { action ->
            actionBreakdown[action.name] ?: 0L
        }

        return AuditSummaryDto(
            totalActions = totalActions,
            actionBreakdown = actionBreakdown,
            mostActiveActors = mostActiveActors,
            sensitiveDataAccesses = sensitiveDataAccesses,
            period = period.name,
        )
    }

    @Transactional
    fun purgeExpiredLogs() {
        val policies = retentionPolicyRepository.findAll()
        var totalRegular = 0
        var totalSensitive = 0

        for (policy in policies) {
            val regularThreshold = OffsetDateTime.now().minusDays(policy.retentionDays.toLong())
            val sensitiveThreshold = OffsetDateTime.now().minusDays(policy.sensitiveDataRetentionDays.toLong())

            val regularDeleted = auditLogRepository.deleteExpiredRegularLogs(
                policy.salonId, regularThreshold, SENSITIVE_ACTIONS,
            )
            val sensitiveDeleted = auditLogRepository.deleteExpiredSensitiveLogs(
                policy.salonId, sensitiveThreshold, SENSITIVE_ACTIONS,
            )

            totalRegular += regularDeleted
            totalSensitive += sensitiveDeleted
        }

        if (totalRegular > 0 || totalSensitive > 0) {
            log.info("Purged audit logs: {} regular, {} sensitive", totalRegular, totalSensitive)
        }
    }

    private fun AuditLog.toDto(): AuditLogDto {
        val parsedMetadata = metadata?.let {
            try {
                @Suppress("UNCHECKED_CAST")
                objectMapper.readValue(it, Map::class.java) as Map<String, Any?>
            } catch (_: Exception) { null }
        }

        val parsedChangedFields = changedFields?.let {
            try {
                @Suppress("UNCHECKED_CAST")
                val list = objectMapper.readValue(it, List::class.java) as List<Map<String, Any?>>
                list.map { m ->
                    FieldChangeDto(
                        field = m["field"] as? String ?: "",
                        oldValue = m["oldValue"],
                        newValue = m["newValue"],
                    )
                }
            } catch (_: Exception) { null }
        }

        return AuditLogDto(
            id = id,
            salonId = salonId,
            actorId = actorId,
            actorEmail = actorEmail,
            actorRole = actorRole,
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            resourceDescription = resourceDescription,
            metadata = parsedMetadata,
            changedFields = parsedChangedFields,
            ipAddress = ipAddress,
            createdAt = createdAt,
        )
    }
}

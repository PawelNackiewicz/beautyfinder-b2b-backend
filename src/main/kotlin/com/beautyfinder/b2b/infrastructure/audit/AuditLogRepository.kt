package com.beautyfinder.b2b.infrastructure.audit

import com.beautyfinder.b2b.domain.audit.AuditAction
import com.beautyfinder.b2b.domain.audit.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface AuditLogRepository : JpaRepository<AuditLog, UUID> {

    @Query(
        """
        SELECT a FROM AuditLog a WHERE a.salonId = :salonId
        AND (:actorId IS NULL OR a.actorId = :actorId)
        AND (:action IS NULL OR a.action = :action)
        AND (:resourceType IS NULL OR a.resourceType = :resourceType)
        AND (:resourceId IS NULL OR a.resourceId = :resourceId)
        AND (:from IS NULL OR a.createdAt >= :from)
        AND (:to IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
        """,
    )
    fun search(
        @Param("salonId") salonId: UUID,
        @Param("actorId") actorId: UUID?,
        @Param("action") action: AuditAction?,
        @Param("resourceType") resourceType: String?,
        @Param("resourceId") resourceId: UUID?,
        @Param("from") from: OffsetDateTime?,
        @Param("to") to: OffsetDateTime?,
        pageable: Pageable,
    ): Page<AuditLog>

    fun findAllByResourceTypeAndResourceIdAndSalonIdOrderByCreatedAtDesc(
        resourceType: String,
        resourceId: UUID,
        salonId: UUID,
        pageable: Pageable,
    ): Page<AuditLog>

    fun findAllByActorIdAndSalonIdOrderByCreatedAtDesc(
        actorId: UUID,
        salonId: UUID,
        pageable: Pageable,
    ): Page<AuditLog>

    fun findAllByResourceTypeAndResourceIdAndSalonIdAndActionIn(
        resourceType: String,
        resourceId: UUID,
        salonId: UUID,
        actions: List<AuditAction>,
    ): List<AuditLog>

    @Modifying
    @Query(
        """
        DELETE FROM AuditLog a WHERE a.salonId = :salonId
        AND a.createdAt < :threshold
        AND a.action NOT IN :sensitiveActions
        """,
    )
    fun deleteExpiredRegularLogs(
        @Param("salonId") salonId: UUID,
        @Param("threshold") threshold: OffsetDateTime,
        @Param("sensitiveActions") sensitiveActions: List<AuditAction>,
    ): Int

    @Modifying
    @Query(
        """
        DELETE FROM AuditLog a WHERE a.salonId = :salonId
        AND a.createdAt < :sensitiveThreshold
        AND a.action IN :sensitiveActions
        """,
    )
    fun deleteExpiredSensitiveLogs(
        @Param("salonId") salonId: UUID,
        @Param("sensitiveThreshold") sensitiveThreshold: OffsetDateTime,
        @Param("sensitiveActions") sensitiveActions: List<AuditAction>,
    ): Int

    @Query(
        """
        SELECT COUNT(a) FROM AuditLog a WHERE a.salonId = :salonId
        AND a.action = :action AND a.createdAt >= :from
        """,
    )
    fun countByActionSince(
        @Param("salonId") salonId: UUID,
        @Param("action") action: AuditAction,
        @Param("from") from: OffsetDateTime,
    ): Long

    @Query(
        """
        SELECT a.action as action, COUNT(a) as cnt FROM AuditLog a
        WHERE a.salonId = :salonId AND a.createdAt >= :from
        GROUP BY a.action
        """,
    )
    fun countByActionGrouped(
        @Param("salonId") salonId: UUID,
        @Param("from") from: OffsetDateTime,
    ): List<ActionCountProjection>

    @Query(
        """
        SELECT a.actorId as actorId, a.actorEmail as actorEmail, COUNT(a) as actionCount
        FROM AuditLog a WHERE a.salonId = :salonId AND a.createdAt >= :from AND a.actorId IS NOT NULL
        GROUP BY a.actorId, a.actorEmail ORDER BY COUNT(a) DESC
        """,
    )
    fun findMostActiveActors(
        @Param("salonId") salonId: UUID,
        @Param("from") from: OffsetDateTime,
        pageable: Pageable,
    ): List<ActorActivityProjection>
}

interface ActionCountProjection {
    val action: AuditAction
    val cnt: Long
}

interface ActorActivityProjection {
    val actorId: UUID
    val actorEmail: String?
    val actionCount: Long
}

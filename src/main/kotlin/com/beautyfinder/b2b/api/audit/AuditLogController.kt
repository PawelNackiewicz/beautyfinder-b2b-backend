package com.beautyfinder.b2b.api.audit

import com.beautyfinder.b2b.application.audit.AuditLogDto
import com.beautyfinder.b2b.application.audit.AuditLogService
import com.beautyfinder.b2b.application.audit.AuditSearchQuery
import com.beautyfinder.b2b.application.audit.AuditSummaryDto
import com.beautyfinder.b2b.application.audit.StatsPeriod
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.audit.AuditAction
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit Log", description = "Audit log viewing endpoints")
class AuditLogController(
    private val auditLogService: AuditLogService,
) {

    @GetMapping("/logs")
    @Operation(summary = "Search audit logs")
    @PreAuthorize("hasRole('OWNER')")
    fun getLogs(
        @RequestParam(required = false) actorId: UUID?,
        @RequestParam(required = false) action: AuditAction?,
        @RequestParam(required = false) resourceType: String?,
        @RequestParam(required = false) resourceId: UUID?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: OffsetDateTime?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<AuditLogDto> {
        val salonId = TenantContext.getSalonId()
        val pageSize = size.coerceAtMost(100)
        return auditLogService.searchLogs(
            AuditSearchQuery(
                actorId = actorId,
                action = action,
                resourceType = resourceType,
                resourceId = resourceId,
                from = from,
                to = to,
            ),
            salonId,
            PageRequest.of(page, pageSize),
        )
    }

    @GetMapping("/logs/resource/{resourceType}/{resourceId}")
    @Operation(summary = "Get audit history for a resource")
    @PreAuthorize("hasRole('OWNER')")
    fun getResourceHistory(
        @PathVariable resourceType: String,
        @PathVariable resourceId: UUID,
    ): List<AuditLogDto> {
        val salonId = TenantContext.getSalonId()
        return auditLogService.getLogsForResource(resourceType, resourceId, salonId)
    }

    @GetMapping("/logs/actor/{actorId}")
    @Operation(summary = "Get audit history for an actor")
    @PreAuthorize("hasRole('OWNER')")
    fun getActorHistory(
        @PathVariable actorId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<AuditLogDto> {
        val salonId = TenantContext.getSalonId()
        val pageSize = size.coerceAtMost(100)
        return auditLogService.getLogsForActor(actorId, salonId, PageRequest.of(page, pageSize))
    }

    @GetMapping("/logs/sensitive-access/{clientId}")
    @Operation(summary = "Get sensitive data access log for a client (GDPR)")
    @PreAuthorize("hasRole('OWNER')")
    fun getSensitiveAccessLog(@PathVariable clientId: UUID): List<AuditLogDto> {
        val salonId = TenantContext.getSalonId()
        return auditLogService.getSensitiveDataAccessLog(clientId, salonId)
    }

    @GetMapping("/summary")
    @Operation(summary = "Get audit summary statistics")
    @PreAuthorize("hasRole('OWNER')")
    fun getSummary(
        @RequestParam(defaultValue = "LAST_30_DAYS") period: StatsPeriod,
    ): AuditSummaryDto {
        val salonId = TenantContext.getSalonId()
        return auditLogService.getSummary(salonId, period)
    }
}

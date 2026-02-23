package com.beautyfinder.b2b.application.audit

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AuditRetentionScheduler(
    private val auditLogService: AuditLogService,
) {

    private val log = LoggerFactory.getLogger(AuditRetentionScheduler::class.java)

    @Scheduled(cron = "0 0 4 * * *")
    fun purgeExpiredLogs() {
        log.info("Starting audit log retention purge")
        auditLogService.purgeExpiredLogs()
        log.info("Audit log retention purge completed")
    }
}

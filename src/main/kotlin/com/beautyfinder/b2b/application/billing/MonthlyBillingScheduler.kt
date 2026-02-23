package com.beautyfinder.b2b.application.billing

import com.beautyfinder.b2b.domain.billing.SubscriptionStatus
import com.beautyfinder.b2b.infrastructure.billing.SalonSubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class MonthlyBillingScheduler(
    private val billingReportService: BillingReportService,
    private val subscriptionRepository: SalonSubscriptionRepository,
) {

    private val log = LoggerFactory.getLogger(MonthlyBillingScheduler::class.java)

    @Scheduled(cron = "0 0 2 1 * *")
    fun generateMonthlyReports() {
        val previousMonth = YearMonth.now().minusMonths(1)
        val year = previousMonth.year
        val month = previousMonth.monthValue

        log.info("Starting monthly billing report generation for {}/{}", year, month)

        val activeSubscriptions = subscriptionRepository.findAll()
            .filter { it.status == SubscriptionStatus.ACTIVE }

        var successCount = 0
        var errorCount = 0

        for (subscription in activeSubscriptions) {
            try {
                billingReportService.generateMonthlyReport(subscription.salonId, year, month)
                successCount++
            } catch (e: Exception) {
                errorCount++
                log.error("Failed to generate billing report for salon {} ({}/{}): {}",
                    subscription.salonId, year, month, e.message)
            }
        }

        log.info("Monthly billing report generation completed: {} success, {} errors", successCount, errorCount)
    }
}

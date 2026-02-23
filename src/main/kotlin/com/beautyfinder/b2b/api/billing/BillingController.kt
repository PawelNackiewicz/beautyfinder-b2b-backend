package com.beautyfinder.b2b.api.billing

import com.beautyfinder.b2b.application.billing.BillingPeriodReportDto
import com.beautyfinder.b2b.application.billing.BillingPeriodReportSummaryDto
import com.beautyfinder.b2b.application.billing.BillingReportDetailsDto
import com.beautyfinder.b2b.application.billing.BillingReportService
import com.beautyfinder.b2b.application.billing.InvoiceDto
import com.beautyfinder.b2b.application.billing.InvoiceSummaryDto
import com.beautyfinder.b2b.application.billing.RevenueStatsDto
import com.beautyfinder.b2b.application.billing.SalonSubscriptionDto
import com.beautyfinder.b2b.application.billing.StatsPeriod
import com.beautyfinder.b2b.config.TenantContext
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing & Reports")
@Validated
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class BillingController(
    private val billingReportService: BillingReportService,
) {

    @GetMapping("/report")
    fun getReport(
        @RequestParam @Min(2020) @Max(2100) year: Int,
        @RequestParam @Min(1) @Max(12) month: Int,
    ): BillingPeriodReportDto {
        val salonId = TenantContext.getSalonId()
        return billingReportService.getMonthlyReport(salonId, year, month)
    }

    @GetMapping("/report/{reportId}/details")
    fun getReportDetails(@PathVariable reportId: UUID): BillingReportDetailsDto {
        val salonId = TenantContext.getSalonId()
        return billingReportService.getReportDetails(reportId, salonId)
    }

    @GetMapping("/reports")
    fun listReports(@PageableDefault(size = 12) pageable: Pageable): Page<BillingPeriodReportSummaryDto> {
        val salonId = TenantContext.getSalonId()
        return billingReportService.listReports(salonId, pageable)
    }

    @PostMapping("/report/{reportId}/invoice")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    fun createInvoice(@PathVariable reportId: UUID): InvoiceDto {
        val salonId = TenantContext.getSalonId()
        return billingReportService.generateInvoice(reportId, salonId)
    }

    @GetMapping("/invoices")
    fun listInvoices(@PageableDefault(size = 20) pageable: Pageable): Page<InvoiceSummaryDto> {
        val salonId = TenantContext.getSalonId()
        return billingReportService.listInvoices(salonId, pageable)
    }

    @GetMapping("/invoices/{invoiceId}")
    fun getInvoice(@PathVariable invoiceId: UUID): InvoiceDto {
        val salonId = TenantContext.getSalonId()
        return billingReportService.getInvoice(invoiceId, salonId)
    }

    @PatchMapping("/invoices/{invoiceId}/mark-paid")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    fun markInvoiceAsPaid(@PathVariable invoiceId: UUID) {
        val salonId = TenantContext.getSalonId()
        billingReportService.markInvoiceAsPaid(invoiceId, salonId)
    }

    @GetMapping("/stats")
    fun getStats(
        @RequestParam(defaultValue = "CURRENT_MONTH") period: StatsPeriod,
    ): RevenueStatsDto {
        val salonId = TenantContext.getSalonId()
        return billingReportService.getRevenueStats(salonId, period)
    }

    @GetMapping("/subscription")
    fun getSubscription(): SalonSubscriptionDto {
        val salonId = TenantContext.getSalonId()
        return billingReportService.getActiveSubscription(salonId)
    }
}

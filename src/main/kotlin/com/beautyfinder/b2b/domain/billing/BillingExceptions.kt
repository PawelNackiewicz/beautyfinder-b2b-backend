package com.beautyfinder.b2b.domain.billing

import java.util.UUID

open class BillingDomainException(message: String) : RuntimeException(message)

class ReportAlreadyGeneratedException(salonId: UUID, year: Int, month: Int) :
    BillingDomainException("Billing report for salon $salonId already exists for $year/$month")

class InvoiceNotFoundException(id: UUID) :
    BillingDomainException("Invoice not found: $id")

class InvoiceAlreadyPaidException(id: UUID) :
    BillingDomainException("Invoice already paid: $id")

class NoActiveSubscriptionException(salonId: UUID) :
    BillingDomainException("No active subscription for salon: $salonId")

class InvalidBillingPeriodException(message: String) :
    BillingDomainException(message)

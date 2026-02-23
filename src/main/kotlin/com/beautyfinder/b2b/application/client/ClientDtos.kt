package com.beautyfinder.b2b.application.client

import com.beautyfinder.b2b.domain.client.ClientStatus
import com.beautyfinder.b2b.domain.client.ConsentType
import com.beautyfinder.b2b.domain.client.LoyaltyTransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ClientSummaryDto(
    val id: UUID,
    val salonId: UUID,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val email: String?,
    val status: ClientStatus,
    val totalVisits: Int,
    val totalSpent: BigDecimal,
    val lastVisitAt: OffsetDateTime?,
    val loyaltyPoints: Int,
    val isBlacklisted: Boolean,
)

data class ClientCardDto(
    val client: ClientSummaryDto,
    val consents: List<GdprConsentDto>,
    val blacklistEntry: BlacklistEntryDto?,
    val loyaltyBalance: LoyaltyBalanceDto,
    val recentAppointments: List<AppointmentSummaryDto>,
    val stats: ClientStatsDto,
)

data class ClientStatsDto(
    val totalVisits: Int,
    val totalSpent: BigDecimal,
    val avgVisitValue: BigDecimal,
    val firstVisitAt: OffsetDateTime?,
    val lastVisitAt: OffsetDateTime?,
    val favoriteServiceName: String?,
    val favoriteEmployeeName: String?,
)

data class GdprConsentDto(
    val id: UUID,
    val consentType: ConsentType,
    val granted: Boolean,
    val grantedAt: OffsetDateTime,
    val expiresAt: OffsetDateTime?,
    val consentVersion: String,
)

data class BlacklistEntryDto(
    val id: UUID,
    val reason: String,
    val createdAt: OffsetDateTime?,
    val isActive: Boolean,
)

data class LoyaltyBalanceDto(
    val points: Int,
    val totalEarned: Int,
    val totalRedeemed: Int,
)

data class LoyaltyTransactionDto(
    val id: UUID,
    val points: Int,
    val type: LoyaltyTransactionType,
    val appointmentId: UUID?,
    val note: String?,
    val balanceAfter: Int,
    val createdAt: OffsetDateTime?,
)

data class AppointmentSummaryDto(
    val id: UUID,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
    val status: String,
    val serviceName: String?,
    val employeeName: String?,
    val finalPrice: BigDecimal?,
)

data class ClientSearchQuery(
    val phrase: String? = null,
    val status: ClientStatus? = null,
    val hasUpcomingAppointment: Boolean? = null,
)

data class CreateClientRequest(
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val email: String? = null,
    val birthDate: LocalDate? = null,
    val notes: String? = null,
    val preferredEmployeeId: UUID? = null,
)

data class UpdateClientRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val birthDate: LocalDate? = null,
    val notes: String? = null,
    val preferredEmployeeId: UUID? = null,
)

data class GdprConsentRequest(
    val consentType: ConsentType,
    val granted: Boolean,
    val consentVersion: String,
    val ipAddress: String? = null,
    val expiresAt: OffsetDateTime? = null,
)

data class CsvImportResultDto(
    val total: Int,
    val imported: Int,
    val skipped: Int,
    val errors: List<CsvRowError>,
)

data class CsvRowError(
    val rowNumber: Int,
    val rawData: String,
    val errorMessage: String,
)

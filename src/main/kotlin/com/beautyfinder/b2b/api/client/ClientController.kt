package com.beautyfinder.b2b.api.client

import com.beautyfinder.b2b.application.client.ClientCardDto
import com.beautyfinder.b2b.application.client.ClientSearchQuery
import com.beautyfinder.b2b.application.client.ClientService
import com.beautyfinder.b2b.application.client.ClientSummaryDto
import com.beautyfinder.b2b.application.client.CsvImportResultDto
import com.beautyfinder.b2b.application.client.GdprConsentDto
import com.beautyfinder.b2b.application.client.GdprConsentRequest
import com.beautyfinder.b2b.application.client.LoyaltyBalanceDto
import com.beautyfinder.b2b.application.client.LoyaltyTransactionDto
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.client.ClientStatus
import com.beautyfinder.b2b.domain.client.ConsentType
import com.beautyfinder.b2b.domain.client.LoyaltyTransactionType
import com.beautyfinder.b2b.domain.client.SensitiveDataPayload
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients - CRM")
class ClientController(
    private val clientService: ClientService,
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun searchClients(
        @RequestParam(required = false) phrase: String?,
        @RequestParam(required = false) status: ClientStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") @Max(100) size: Int,
    ): Page<ClientSummaryDto> {
        val query = ClientSearchQuery(phrase = phrase, status = status)
        return clientService.searchClients(query, TenantContext.getSalonId(), PageRequest.of(page, size))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getClientCard(@PathVariable id: UUID): ClientCardDto {
        return clientService.getClientCard(id, TenantContext.getSalonId())
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun createClient(@RequestBody @Valid request: CreateClientApiRequest): ClientSummaryDto {
        return clientService.createClient(
            com.beautyfinder.b2b.application.client.CreateClientRequest(
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone,
                email = request.email,
                birthDate = request.birthDate,
                notes = request.notes,
                preferredEmployeeId = request.preferredEmployeeId,
            ),
            TenantContext.getSalonId(),
        )
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun updateClient(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateClientApiRequest,
    ): ClientSummaryDto {
        return clientService.updateClient(
            id,
            com.beautyfinder.b2b.application.client.UpdateClientRequest(
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone,
                email = request.email,
                birthDate = request.birthDate,
                notes = request.notes,
                preferredEmployeeId = request.preferredEmployeeId,
            ),
            TenantContext.getSalonId(),
        )
    }

    @GetMapping("/{id}/sensitive-data")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun getSensitiveData(@PathVariable id: UUID): ResponseEntity<SensitiveDataPayload> {
        val payload = clientService.getSensitiveData(id, TenantContext.getSalonId())
        return ResponseEntity.ok()
            .header("X-Audit-Access", "true")
            .body(payload)
    }

    @PutMapping("/{id}/sensitive-data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun upsertSensitiveData(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpsertSensitiveDataApiRequest,
    ) {
        clientService.upsertSensitiveData(
            id,
            SensitiveDataPayload(
                pesel = request.pesel,
                idNumber = request.idNumber,
                medicalNotes = request.medicalNotes,
                allergyInfo = request.allergyInfo,
                customFields = request.customFields ?: emptyMap(),
            ),
            TenantContext.getSalonId(),
        )
    }

    @PostMapping("/{id}/gdpr-consent")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun recordConsent(
        @PathVariable id: UUID,
        @RequestBody @Valid request: GdprConsentApiRequest,
    ): GdprConsentDto {
        return clientService.recordGdprConsent(
            id,
            GdprConsentRequest(
                consentType = request.consentType,
                granted = request.granted,
                consentVersion = request.consentVersion,
                ipAddress = request.ipAddress,
                expiresAt = request.expiresAt,
            ),
            TenantContext.getSalonId(),
        )
    }

    @DeleteMapping("/{id}/gdpr-consent/{consentType}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun revokeConsent(
        @PathVariable id: UUID,
        @PathVariable consentType: ConsentType,
    ) {
        clientService.revokeGdprConsent(id, consentType, TenantContext.getSalonId())
    }

    @PostMapping("/{id}/blacklist")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun addToBlacklist(
        @PathVariable id: UUID,
        @RequestBody @Valid request: BlacklistApiRequest,
    ) {
        clientService.addToBlacklist(
            id,
            request.reason,
            // In a real app, extract userId from SecurityContext
            UUID.randomUUID(),
            TenantContext.getSalonId(),
        )
    }

    @DeleteMapping("/{id}/blacklist")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun removeFromBlacklist(@PathVariable id: UUID) {
        clientService.removeFromBlacklist(
            id,
            UUID.randomUUID(),
            TenantContext.getSalonId(),
        )
    }

    @GetMapping("/{id}/loyalty")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getLoyalty(@PathVariable id: UUID): LoyaltyResponse {
        val (balance, transactions) = clientService.getLoyaltyDetails(id, TenantContext.getSalonId())
        return LoyaltyResponse(balance = balance, transactions = transactions)
    }

    @PostMapping("/{id}/loyalty/adjust")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun adjustLoyalty(
        @PathVariable id: UUID,
        @RequestBody @Valid request: ManualLoyaltyApiRequest,
    ): LoyaltyBalanceDto {
        val type = if (request.points > 0) LoyaltyTransactionType.MANUAL_ADD
        else LoyaltyTransactionType.MANUAL_DEDUCT

        val balance = clientService.addLoyaltyPoints(
            id, request.points, type, null, request.note, TenantContext.getSalonId()
        )
        return LoyaltyBalanceDto(
            points = balance.points,
            totalEarned = balance.totalEarned,
            totalRedeemed = balance.totalRedeemed,
        )
    }
}

@RestController
@RequestMapping("/api/import")
@Tag(name = "Clients - CRM")
class ClientImportController(
    private val clientService: ClientService,
) {

    @PostMapping("/clients")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun importCsv(@RequestParam file: MultipartFile): CsvImportResultDto {
        require(file.contentType == "text/csv") { "Only CSV files are accepted" }
        return clientService.importClientsFromCsv(file.inputStream, TenantContext.getSalonId())
    }
}

// -- API Request DTOs with validation --

data class CreateClientApiRequest(
    @field:NotBlank @field:Size(max = 100)
    val firstName: String,

    @field:NotBlank @field:Size(max = 100)
    val lastName: String,

    @field:Pattern(regexp = "\\+?[0-9]{9,15}")
    val phone: String? = null,

    @field:Email @field:Size(max = 200)
    val email: String? = null,

    @field:Past
    val birthDate: LocalDate? = null,

    @field:Size(max = 2000)
    val notes: String? = null,

    val preferredEmployeeId: UUID? = null,
) {
    @Suppress("unused")
    fun isAtLeastOneContactProvided(): Boolean = phone != null || email != null
}

data class UpdateClientApiRequest(
    @field:Size(max = 100)
    val firstName: String? = null,

    @field:Size(max = 100)
    val lastName: String? = null,

    @field:Pattern(regexp = "\\+?[0-9]{9,15}")
    val phone: String? = null,

    @field:Email @field:Size(max = 200)
    val email: String? = null,

    @field:Past
    val birthDate: LocalDate? = null,

    @field:Size(max = 2000)
    val notes: String? = null,

    val preferredEmployeeId: UUID? = null,
)

data class UpsertSensitiveDataApiRequest(
    @field:Pattern(regexp = "[0-9]{11}")
    val pesel: String? = null,

    @field:Size(max = 20)
    val idNumber: String? = null,

    @field:Size(max = 5000)
    val medicalNotes: String? = null,

    @field:Size(max = 2000)
    val allergyInfo: String? = null,

    @field:Size(max = 20)
    val customFields: Map<String, String>? = null,
)

data class GdprConsentApiRequest(
    @field:NotNull
    val consentType: ConsentType,

    @field:NotNull
    val granted: Boolean,

    @field:NotBlank @field:Size(max = 10)
    val consentVersion: String,

    @field:Size(max = 45)
    val ipAddress: String? = null,

    @field:Future
    val expiresAt: OffsetDateTime? = null,
)

data class BlacklistApiRequest(
    @field:NotBlank @field:Size(max = 500)
    val reason: String,
)

data class ManualLoyaltyApiRequest(
    @field:NotNull
    val points: Int,

    @field:Size(max = 200)
    val note: String? = null,
)

data class LoyaltyResponse(
    val balance: LoyaltyBalanceDto,
    val transactions: List<LoyaltyTransactionDto>,
)

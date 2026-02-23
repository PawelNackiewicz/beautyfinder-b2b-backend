package com.beautyfinder.b2b.api.salon

import com.beautyfinder.b2b.application.salon.SalonSettingsService
import com.beautyfinder.b2b.config.TenantContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/salon")
@Tag(name = "Salon Settings", description = "Salon settings management")
class SalonSettingsController(
    private val salonSettingsService: SalonSettingsService,
) {

    @GetMapping("/settings")
    @Operation(summary = "Get salon settings")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun getSettings(): SalonSettingsResponse {
        val salonId = TenantContext.getSalonId()
        return salonSettingsService.getSalonSettings(salonId).toResponse()
    }

    @PutMapping("/settings/general")
    @Operation(summary = "Update general settings")
    @PreAuthorize("hasRole('OWNER')")
    fun updateGeneralSettings(@Valid @RequestBody request: UpdateGeneralSettingsApiRequest): SalonSettingsResponse {
        val salonId = TenantContext.getSalonId()
        return salonSettingsService.updateGeneralSettings(salonId, request.toServiceRequest()).toResponse()
    }

    @PutMapping("/settings/opening-hours")
    @Operation(summary = "Update opening hours")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun updateOpeningHours(@Valid @RequestBody request: List<UpdateOpeningHoursApiRequest>): List<SalonOpeningHoursResponse> {
        val salonId = TenantContext.getSalonId()
        return salonSettingsService.updateOpeningHours(salonId, request.map { it.toServiceRequest() })
            .map { it.toResponse() }
    }

    @PutMapping("/settings/invoicing")
    @Operation(summary = "Update invoicing settings")
    @PreAuthorize("hasRole('OWNER')")
    fun updateInvoicingSettings(@Valid @RequestBody request: UpdateInvoicingSettingsApiRequest): SalonSettingsResponse {
        val salonId = TenantContext.getSalonId()
        return salonSettingsService.updateInvoicingSettings(salonId, request.toServiceRequest()).toResponse()
    }

    @PutMapping("/settings/loyalty")
    @Operation(summary = "Update loyalty settings")
    @PreAuthorize("hasRole('OWNER')")
    fun updateLoyaltySettings(@Valid @RequestBody request: UpdateLoyaltySettingsApiRequest): SalonSettingsResponse {
        val salonId = TenantContext.getSalonId()
        return salonSettingsService.updateLoyaltySettings(salonId, request.toServiceRequest()).toResponse()
    }

    @PutMapping("/settings/notifications")
    @Operation(summary = "Update notification settings")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun updateNotificationSettings(@Valid @RequestBody request: UpdateNotificationSettingsApiRequest): SalonNotificationSettingsResponse {
        val salonId = TenantContext.getSalonId()
        return salonSettingsService.updateNotificationSettings(salonId, request.toServiceRequest()).toResponse()
    }

    @PutMapping("/settings/integrations")
    @Operation(summary = "Update integration settings")
    @PreAuthorize("hasRole('OWNER')")
    fun updateIntegrationSettings(@Valid @RequestBody request: UpdateIntegrationSettingsApiRequest): SalonIntegrationSettingsResponse {
        val salonId = TenantContext.getSalonId()
        return salonSettingsService.updateIntegrationSettings(salonId, request.toServiceRequest()).toResponse()
    }

    @PostMapping("/settings/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload salon logo")
    @PreAuthorize("hasRole('OWNER')")
    fun uploadLogo(@RequestParam file: MultipartFile): LogoUploadResponse {
        val salonId = TenantContext.getSalonId()
        val logoUrl = salonSettingsService.uploadLogo(salonId, file)
        return LogoUploadResponse(logoUrl = logoUrl)
    }

    @GetMapping("/public/{slug}")
    @Operation(summary = "Get public salon profile")
    fun getPublicProfile(@PathVariable slug: String): SalonPublicProfileResponse {
        return salonSettingsService.getSalonPublicProfile(slug).toResponse()
    }
}

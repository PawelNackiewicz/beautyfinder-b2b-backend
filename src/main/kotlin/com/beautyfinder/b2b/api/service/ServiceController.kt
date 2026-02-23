package com.beautyfinder.b2b.api.service

import com.beautyfinder.b2b.application.service.ServiceCategoryDto
import com.beautyfinder.b2b.application.service.ServiceService
import com.beautyfinder.b2b.application.service.ServiceVariantDto
import com.beautyfinder.b2b.application.service.ServiceWithVariantsDto
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.service.VariantStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
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
import java.math.BigDecimal
import java.util.UUID
import com.beautyfinder.b2b.application.service.CreateServiceRequest as ServiceCreateServiceRequest
import com.beautyfinder.b2b.application.service.CreateVariantRequest as ServiceCreateVariantRequest
import com.beautyfinder.b2b.application.service.UpdateServiceRequest as ServiceUpdateServiceRequest
import com.beautyfinder.b2b.application.service.UpdateVariantRequest as ServiceUpdateVariantRequest
import com.beautyfinder.b2b.application.service.CreateCategoryRequest as ServiceCreateCategoryRequest
import com.beautyfinder.b2b.application.service.UpdateCategoryRequest as ServiceUpdateCategoryRequest
import com.beautyfinder.b2b.application.service.ReorderRequest as ServiceReorderRequest

// --- API Request DTOs ---

data class CreateServiceApiRequest(
    @field:NotBlank @field:Size(max = 100)
    val name: String,

    @field:NotBlank @field:Size(max = 50)
    val category: String,

    val categoryId: UUID? = null,

    @field:Size(max = 1000)
    val description: String? = null,

    val isOnlineBookable: Boolean = true,

    @field:Size(max = 20)
    val variants: List<@Valid CreateVariantApiRequest>? = null,
)

data class UpdateServiceApiRequest(
    @field:Size(max = 100)
    val name: String? = null,

    @field:Size(max = 50)
    val category: String? = null,

    val categoryId: UUID? = null,

    @field:Size(max = 1000)
    val description: String? = null,

    @field:Size(max = 500)
    val imageUrl: String? = null,

    @field:Min(0)
    val displayOrder: Int? = null,

    val isOnlineBookable: Boolean? = null,
)

data class CreateVariantApiRequest(
    @field:NotBlank @field:Size(max = 100)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    @field:NotNull @field:Min(5) @field:Max(480)
    val durationMinutes: Int,

    @field:NotNull @field:DecimalMin("0.00")
    val price: BigDecimal,

    @field:DecimalMin("0.00")
    val priceMax: BigDecimal? = null,

    val isOnlineBookable: Boolean = true,
) {
    @Suppress("unused")
    fun isDurationValid(): Boolean = durationMinutes % 5 == 0
}

data class UpdateVariantApiRequest(
    @field:Size(max = 100)
    val name: String? = null,

    @field:Size(max = 500)
    val description: String? = null,

    @field:Min(5) @field:Max(480)
    val durationMinutes: Int? = null,

    @field:DecimalMin("0.00")
    val price: BigDecimal? = null,

    @field:DecimalMin("0.00")
    val priceMax: BigDecimal? = null,

    val isOnlineBookable: Boolean? = null,

    val status: VariantStatus? = null,
)

data class CreateCategoryApiRequest(
    @field:NotBlank @field:Size(max = 50)
    val name: String,

    @field:Min(0)
    val displayOrder: Int? = null,

    @field:Pattern(regexp = "#[0-9A-Fa-f]{6}")
    val colorHex: String? = null,

    @field:Size(max = 50)
    val iconName: String? = null,
)

data class UpdateCategoryApiRequest(
    @field:Size(max = 50)
    val name: String? = null,

    @field:Min(0)
    val displayOrder: Int? = null,

    @field:Pattern(regexp = "#[0-9A-Fa-f]{6}")
    val colorHex: String? = null,

    @field:Size(max = 50)
    val iconName: String? = null,
)

data class ReorderApiRequest(
    @field:NotEmpty @field:Size(max = 100)
    val orderedIds: List<UUID>,
)

// --- Controller ---

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Service management endpoints")
class ServiceController(
    private val serviceService: ServiceService,
) {

    @GetMapping
    @Operation(summary = "List all services with variants")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun listServices(
        @RequestParam(defaultValue = "false") includeInactive: Boolean,
    ): List<ServiceWithVariantsDto> {
        val salonId = TenantContext.getSalonId()
        return serviceService.listServices(salonId, includeInactive)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service with variants")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun getService(@PathVariable id: UUID): ServiceWithVariantsDto {
        val salonId = TenantContext.getSalonId()
        return serviceService.getService(id, salonId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new service")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun createService(@Valid @RequestBody request: CreateServiceApiRequest): ServiceWithVariantsDto {
        val salonId = TenantContext.getSalonId()
        return serviceService.createService(
            ServiceCreateServiceRequest(
                name = request.name,
                category = request.category,
                categoryId = request.categoryId,
                description = request.description,
                isOnlineBookable = request.isOnlineBookable,
                variants = request.variants?.map {
                    ServiceCreateVariantRequest(
                        name = it.name,
                        description = it.description,
                        durationMinutes = it.durationMinutes,
                        price = it.price,
                        priceMax = it.priceMax,
                        isOnlineBookable = it.isOnlineBookable,
                    )
                },
            ),
            salonId,
        )
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update service")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun updateService(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateServiceApiRequest,
    ): ServiceWithVariantsDto {
        val salonId = TenantContext.getSalonId()
        return serviceService.updateService(
            id,
            ServiceUpdateServiceRequest(
                name = request.name,
                category = request.category,
                categoryId = request.categoryId,
                description = request.description,
                imageUrl = request.imageUrl,
                displayOrder = request.displayOrder,
                isOnlineBookable = request.isOnlineBookable,
            ),
            salonId,
        )
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive service (soft delete)")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun archiveService(@PathVariable id: UUID) {
        val salonId = TenantContext.getSalonId()
        serviceService.archiveService(id, salonId)
    }

    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a variant for a service")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun createVariant(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateVariantApiRequest,
    ): ServiceVariantDto {
        val salonId = TenantContext.getSalonId()
        return serviceService.createVariant(
            id,
            ServiceCreateVariantRequest(
                name = request.name,
                description = request.description,
                durationMinutes = request.durationMinutes,
                price = request.price,
                priceMax = request.priceMax,
                isOnlineBookable = request.isOnlineBookable,
            ),
            salonId,
        )
    }

    @PutMapping("/{serviceId}/variants/{variantId}")
    @Operation(summary = "Update a variant")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun updateVariant(
        @PathVariable serviceId: UUID,
        @PathVariable variantId: UUID,
        @Valid @RequestBody request: UpdateVariantApiRequest,
    ): ServiceVariantDto {
        val salonId = TenantContext.getSalonId()
        return serviceService.updateVariant(
            variantId,
            ServiceUpdateVariantRequest(
                name = request.name,
                description = request.description,
                durationMinutes = request.durationMinutes,
                price = request.price,
                priceMax = request.priceMax,
                isOnlineBookable = request.isOnlineBookable,
                status = request.status,
            ),
            salonId,
        )
    }

    @DeleteMapping("/{serviceId}/variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a variant")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun deactivateVariant(
        @PathVariable serviceId: UUID,
        @PathVariable variantId: UUID,
    ) {
        val salonId = TenantContext.getSalonId()
        serviceService.deactivateVariant(variantId, salonId)
    }

    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reorder services")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun reorderServices(@Valid @RequestBody request: ReorderApiRequest) {
        val salonId = TenantContext.getSalonId()
        serviceService.reorderServices(salonId, request.orderedIds)
    }

    @PostMapping("/{id}/variants/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reorder variants within a service")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun reorderVariants(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReorderApiRequest,
    ) {
        val salonId = TenantContext.getSalonId()
        serviceService.reorderVariants(id, salonId, request.orderedIds)
    }

    // --- Categories ---

    @GetMapping("/categories")
    @Operation(summary = "List service categories")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'EMPLOYEE')")
    fun listCategories(): List<ServiceCategoryDto> {
        val salonId = TenantContext.getSalonId()
        return serviceService.listCategories(salonId)
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a service category")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun createCategory(@Valid @RequestBody request: CreateCategoryApiRequest): ServiceCategoryDto {
        val salonId = TenantContext.getSalonId()
        return serviceService.createCategory(
            ServiceCreateCategoryRequest(
                name = request.name,
                displayOrder = request.displayOrder,
                colorHex = request.colorHex,
                iconName = request.iconName,
            ),
            salonId,
        )
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a service category")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun updateCategory(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCategoryApiRequest,
    ): ServiceCategoryDto {
        val salonId = TenantContext.getSalonId()
        return serviceService.updateCategory(
            id,
            ServiceUpdateCategoryRequest(
                name = request.name,
                displayOrder = request.displayOrder,
                colorHex = request.colorHex,
                iconName = request.iconName,
            ),
            salonId,
        )
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a service category")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    fun deleteCategory(@PathVariable id: UUID) {
        val salonId = TenantContext.getSalonId()
        serviceService.deleteCategory(id, salonId)
    }
}

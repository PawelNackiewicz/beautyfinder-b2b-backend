package com.beautyfinder.b2b.application.service

import com.beautyfinder.b2b.domain.service.PriceRange
import com.beautyfinder.b2b.domain.service.Duration
import com.beautyfinder.b2b.domain.service.ServiceStatus
import com.beautyfinder.b2b.domain.service.VariantStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ServiceWithVariantsDto(
    val id: UUID,
    val salonId: UUID,
    val name: String,
    val category: String,
    val categoryId: UUID?,
    val description: String?,
    val imageUrl: String?,
    val displayOrder: Int,
    val status: ServiceStatus,
    val isOnlineBookable: Boolean,
    val variants: List<ServiceVariantDto>,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

data class ServiceVariantDto(
    val id: UUID,
    val serviceId: UUID,
    val salonId: UUID,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val durationFormatted: String,
    val price: BigDecimal,
    val priceMax: BigDecimal?,
    val priceFormatted: String,
    val displayOrder: Int,
    val status: VariantStatus,
    val isOnlineBookable: Boolean,
)

data class ServiceCategoryDto(
    val id: UUID,
    val salonId: UUID,
    val name: String,
    val displayOrder: Int,
    val colorHex: String?,
    val iconName: String?,
)

data class ServiceSummaryDto(
    val id: UUID,
    val name: String,
    val category: String,
    val variantCount: Int,
    val priceFrom: BigDecimal?,
    val priceTo: BigDecimal?,
    val durationFrom: Int?,
    val durationTo: Int?,
)

// --- Request DTOs ---

data class CreateServiceRequest(
    val name: String,
    val category: String,
    val categoryId: UUID? = null,
    val description: String? = null,
    val isOnlineBookable: Boolean = true,
    val variants: List<CreateVariantRequest>? = null,
)

data class UpdateServiceRequest(
    val name: String? = null,
    val category: String? = null,
    val categoryId: UUID? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val displayOrder: Int? = null,
    val isOnlineBookable: Boolean? = null,
)

data class CreateVariantRequest(
    val name: String,
    val description: String? = null,
    val durationMinutes: Int,
    val price: BigDecimal,
    val priceMax: BigDecimal? = null,
    val isOnlineBookable: Boolean = true,
)

data class UpdateVariantRequest(
    val name: String? = null,
    val description: String? = null,
    val durationMinutes: Int? = null,
    val price: BigDecimal? = null,
    val priceMax: BigDecimal? = null,
    val isOnlineBookable: Boolean? = null,
    val status: VariantStatus? = null,
)

data class CreateCategoryRequest(
    val name: String,
    val displayOrder: Int? = null,
    val colorHex: String? = null,
    val iconName: String? = null,
)

data class UpdateCategoryRequest(
    val name: String? = null,
    val displayOrder: Int? = null,
    val colorHex: String? = null,
    val iconName: String? = null,
)

data class ReorderRequest(
    val orderedIds: List<UUID>,
)

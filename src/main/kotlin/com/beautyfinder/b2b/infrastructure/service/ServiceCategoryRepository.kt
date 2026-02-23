package com.beautyfinder.b2b.infrastructure.service

import com.beautyfinder.b2b.domain.service.ServiceCategory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ServiceCategoryRepository : JpaRepository<ServiceCategory, UUID> {

    fun findAllBySalonIdOrderByDisplayOrderAsc(salonId: UUID): List<ServiceCategory>

    fun findByIdAndSalonId(id: UUID, salonId: UUID): ServiceCategory?

    fun existsByNameAndSalonId(name: String, salonId: UUID): Boolean

    fun existsByNameAndSalonIdAndIdNot(name: String, salonId: UUID, excludeId: UUID): Boolean
}

package com.beautyfinder.b2b.infrastructure.service

import com.beautyfinder.b2b.domain.service.Service
import com.beautyfinder.b2b.domain.service.ServiceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ServiceRepository : JpaRepository<Service, UUID> {

    fun findByIdAndSalonId(id: UUID, salonId: UUID): Service?

    fun findAllBySalonIdAndStatusNotOrderByDisplayOrderAsc(salonId: UUID, status: ServiceStatus): List<Service>

    fun findAllBySalonId(salonId: UUID): List<Service>

    fun findAllBySalonIdOrderByDisplayOrderAsc(salonId: UUID): List<Service>

    fun existsByNameAndSalonIdAndIdNot(name: String, salonId: UUID, excludeId: UUID): Boolean

    fun existsByNameAndSalonId(name: String, salonId: UUID): Boolean

    @Query("SELECT MAX(s.displayOrder) FROM Service s WHERE s.salonId = :salonId")
    fun findMaxDisplayOrder(@Param("salonId") salonId: UUID): Int?

    @Modifying
    @Query("UPDATE Service s SET s.displayOrder = :order WHERE s.id = :id")
    fun updateDisplayOrder(@Param("id") id: UUID, @Param("order") order: Int)

    fun findAllByCategoryId(categoryId: UUID): List<Service>

    @Modifying
    @Query("UPDATE Service s SET s.categoryId = null, s.category = '' WHERE s.categoryId = :categoryId")
    fun clearCategoryForServices(@Param("categoryId") categoryId: UUID)
}

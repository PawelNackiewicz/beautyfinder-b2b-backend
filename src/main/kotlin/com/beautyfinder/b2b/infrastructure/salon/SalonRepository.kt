package com.beautyfinder.b2b.infrastructure.salon

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.salon.SalonStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SalonRepository : JpaRepository<Salon, UUID> {
    fun findBySlug(slug: String): Salon?
    fun existsBySlugAndIdNot(slug: String, excludeId: UUID): Boolean
    fun findByStatus(status: SalonStatus): List<Salon>
    fun findByIdAndStatus(id: UUID, status: SalonStatus): Salon?
}

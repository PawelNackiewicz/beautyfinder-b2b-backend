package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.client.Client
import com.beautyfinder.b2b.domain.client.ClientStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

interface ClientRepository : JpaRepository<Client, UUID> {

    fun findAllBySalonId(salonId: UUID): List<Client>

    fun findByIdAndSalonId(id: UUID, salonId: UUID): Client?

    fun findByPhoneAndSalonId(phone: String, salonId: UUID): Client?

    fun findByEmailAndSalonId(email: String, salonId: UUID): Client?

    fun existsByPhoneAndSalonIdAndIdNot(phone: String, salonId: UUID, excludeId: UUID): Boolean

    fun existsByEmailAndSalonIdAndIdNot(email: String, salonId: UUID, excludeId: UUID): Boolean

    fun existsByPhoneAndSalonId(phone: String, salonId: UUID): Boolean

    fun existsByEmailAndSalonId(email: String, salonId: UUID): Boolean

    @Query(
        """
        SELECT c FROM Client c WHERE c.salonId = :salonId
        AND c.status != 'DELETED'
        AND (:phrase IS NULL OR (
            LOWER(c.firstName) LIKE LOWER(CONCAT('%',:phrase,'%')) OR
            LOWER(c.lastName) LIKE LOWER(CONCAT('%',:phrase,'%')) OR
            c.phone LIKE CONCAT('%',:phrase,'%') OR
            LOWER(c.email) LIKE LOWER(CONCAT('%',:phrase,'%'))
        ))
        AND (:status IS NULL OR c.status = :status)
        ORDER BY c.lastName ASC, c.firstName ASC
        """
    )
    fun searchClients(
        @Param("salonId") salonId: UUID,
        @Param("phrase") phrase: String?,
        @Param("status") status: ClientStatus?,
        pageable: Pageable,
    ): Page<Client>

    @Modifying
    @Query(
        """
        UPDATE Client c SET c.totalVisits = c.totalVisits + 1,
        c.totalSpent = c.totalSpent + :amount, c.lastVisitAt = :visitAt
        WHERE c.id = :clientId
        """
    )
    fun updateStatsAfterVisit(
        @Param("clientId") clientId: UUID,
        @Param("amount") amount: BigDecimal,
        @Param("visitAt") visitAt: OffsetDateTime,
    )
}

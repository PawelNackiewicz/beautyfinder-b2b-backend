package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.client.ConsentType
import com.beautyfinder.b2b.domain.client.GdprConsent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GdprConsentRepository : JpaRepository<GdprConsent, UUID> {

    fun findAllByClientIdAndSalonId(clientId: UUID, salonId: UUID): List<GdprConsent>

    fun findByClientIdAndConsentTypeAndGrantedTrue(clientId: UUID, consentType: ConsentType): GdprConsent?

    fun findAllByClientIdOrderByGrantedAtDesc(clientId: UUID): List<GdprConsent>
}

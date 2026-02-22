package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.Salon
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SalonRepository : JpaRepository<Salon, UUID>

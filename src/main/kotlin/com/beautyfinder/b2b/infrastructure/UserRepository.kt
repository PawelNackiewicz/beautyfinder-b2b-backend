package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
}

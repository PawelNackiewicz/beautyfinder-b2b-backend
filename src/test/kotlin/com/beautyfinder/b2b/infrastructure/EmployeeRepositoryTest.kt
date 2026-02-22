package com.beautyfinder.b2b.infrastructure

import com.beautyfinder.b2b.domain.Salon
import com.beautyfinder.b2b.domain.User
import com.beautyfinder.b2b.domain.UserRole
import com.beautyfinder.b2b.domain.employee.Employee
import com.beautyfinder.b2b.domain.employee.EmployeeStatus
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EmployeeRepositoryTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var salon: Salon
    private lateinit var salon2: Salon
    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        salon = Salon(name = "Salon 1", slug = "salon-1-${UUID.randomUUID()}")
        entityManager.persist(salon)

        salon2 = Salon(name = "Salon 2", slug = "salon-2-${UUID.randomUUID()}")
        entityManager.persist(salon2)

        user = User(salonId = salon.id!!, role = UserRole.EMPLOYEE, email = "emp-${UUID.randomUUID()}@test.com", passwordHash = "hash")
        entityManager.persist(user)

        entityManager.flush()
    }

    @Test
    fun `findAllBySalonIdAndStatusNot filters deleted employees`() {
        // given
        val active = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "Active", status = EmployeeStatus.ACTIVE)
        val deleted = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "Deleted", status = EmployeeStatus.DELETED)
        entityManager.persist(active)
        entityManager.persist(deleted)
        entityManager.flush()

        // when
        val results = employeeRepository.findAllBySalonIdAndStatusNotOrderByDisplayNameAsc(salon.id!!, EmployeeStatus.DELETED)

        // then
        assertEquals(1, results.size)
        assertEquals("Active", results[0].displayName)
    }

    @Test
    fun `findByUserIdAndSalonId returns employee`() {
        // given
        val employee = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "John")
        entityManager.persist(employee)
        entityManager.flush()

        // when
        val result = employeeRepository.findByUserIdAndSalonId(user.id!!, salon.id!!)

        // then
        assertNotNull(result)
        assertEquals("John", result!!.displayName)
    }

    @Test
    fun `existsByUserIdAndSalonIdAndStatusNot returns true for active employee`() {
        // given
        val employee = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "John", status = EmployeeStatus.ACTIVE)
        entityManager.persist(employee)
        entityManager.flush()

        // when
        val exists = employeeRepository.existsByUserIdAndSalonIdAndStatusNot(user.id!!, salon.id!!, EmployeeStatus.DELETED)

        // then
        assertTrue(exists)
    }

    @Test
    fun `findByIdAndSalonId wrong salon returns null (multi-tenancy test)`() {
        // given
        val employee = Employee(salonId = salon.id!!, userId = user.id!!, displayName = "John")
        entityManager.persist(employee)
        entityManager.flush()

        // when
        val result = employeeRepository.findByIdAndSalonId(employee.id!!, salon2.id!!)

        // then
        assertNull(result)
    }
}

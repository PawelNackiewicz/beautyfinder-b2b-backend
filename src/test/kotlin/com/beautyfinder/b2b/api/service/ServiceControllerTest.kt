package com.beautyfinder.b2b.api.service

import com.beautyfinder.b2b.application.service.ServiceCategoryDto
import com.beautyfinder.b2b.application.service.ServiceService
import com.beautyfinder.b2b.application.service.ServiceVariantDto
import com.beautyfinder.b2b.application.service.ServiceWithVariantsDto
import com.beautyfinder.b2b.config.JwtAuthenticationFilter
import com.beautyfinder.b2b.config.JwtService
import com.beautyfinder.b2b.config.SecurityConfig
import com.beautyfinder.b2b.config.TenantContext
import com.beautyfinder.b2b.domain.service.CannotArchiveServiceWithActiveAppointmentsException
import com.beautyfinder.b2b.domain.service.DuplicateCategoryNameException
import com.beautyfinder.b2b.domain.service.DuplicateServiceNameException
import com.beautyfinder.b2b.domain.service.InvalidDurationException
import com.beautyfinder.b2b.domain.service.ServiceStatus
import com.beautyfinder.b2b.domain.service.VariantStatus
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@WebMvcTest(ServiceController::class)
@Import(ServiceControllerTest.MockConfig::class, SecurityConfig::class)
class ServiceControllerTest {

    @TestConfiguration
    class MockConfig {
        @Bean
        fun serviceService(): ServiceService = mockk(relaxed = true)

        @Bean
        fun jwtService(): JwtService = mockk()

        @Bean
        fun jwtAuthenticationFilter(jwtService: JwtService): JwtAuthenticationFilter =
            JwtAuthenticationFilter(jwtService)
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var serviceService: ServiceService

    private val salonId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mockkObject(TenantContext)
        every { TenantContext.getSalonId() } returns salonId
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TenantContext)
    }

    private fun buildServiceDto(id: UUID = UUID.randomUUID()) = ServiceWithVariantsDto(
        id = id, salonId = salonId, name = "Strzyżenie", category = "Fryzjerstwo",
        categoryId = null, description = null, imageUrl = null, displayOrder = 0,
        status = ServiceStatus.ACTIVE, isOnlineBookable = true, variants = emptyList(),
        createdAt = OffsetDateTime.now(), updatedAt = OffsetDateTime.now(),
    )

    private fun buildVariantDto(id: UUID = UUID.randomUUID()) = ServiceVariantDto(
        id = id, serviceId = UUID.randomUUID(), salonId = salonId, name = "Standard",
        description = null, durationMinutes = 30, durationFormatted = "30 min",
        price = BigDecimal("50.00"), priceMax = null, priceFormatted = "50 PLN",
        displayOrder = 0, status = VariantStatus.ACTIVE, isOnlineBookable = true,
    )

    private fun buildCategoryDto(id: UUID = UUID.randomUUID()) = ServiceCategoryDto(
        id = id, salonId = salonId, name = "Włosy", displayOrder = 0,
        colorHex = "#FF0000", iconName = "scissors",
    )

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `listServices returns 200 with grouped services`() {
        every { serviceService.listServices(salonId, false) } returns listOf(buildServiceDto())

        mockMvc.get("/api/services")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].name") { value("Strzyżenie") }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `listServices includeInactive returns 200`() {
        every { serviceService.listServices(salonId, true) } returns listOf(buildServiceDto())

        mockMvc.get("/api/services?includeInactive=true")
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createService valid request returns 201`() {
        every { serviceService.createService(any(), eq(salonId)) } returns buildServiceDto()

        val request = mapOf(
            "name" to "Strzyżenie",
            "category" to "Fryzjerstwo",
        )

        mockMvc.post("/api/services") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createService duplicate name returns 409`() {
        every { serviceService.createService(any(), eq(salonId)) } throws
            DuplicateServiceNameException("Strzyżenie", salonId)

        val request = mapOf("name" to "Strzyżenie", "category" to "Fryzjerstwo")

        mockMvc.post("/api/services") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createService invalid variant duration returns 400`() {
        every { serviceService.createService(any(), eq(salonId)) } throws InvalidDurationException(17)

        val request = mapOf(
            "name" to "Test",
            "category" to "Cat",
            "variants" to listOf(
                mapOf("name" to "V1", "durationMinutes" to 17, "price" to 50),
            ),
        )

        mockMvc.post("/api/services") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `updateService returns 200`() {
        every { serviceService.updateService(any(), any(), eq(salonId)) } returns buildServiceDto()

        val request = mapOf("name" to "Nowa nazwa")

        mockMvc.put("/api/services/${UUID.randomUUID()}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `archiveService has future appointments returns 422`() {
        val id = UUID.randomUUID()
        every { serviceService.archiveService(id, salonId) } throws
            CannotArchiveServiceWithActiveAppointmentsException(id, 3)

        mockMvc.delete("/api/services/$id")
            .andExpect {
                status { isUnprocessableEntity() }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createVariant valid returns 201`() {
        every { serviceService.createVariant(any(), any(), eq(salonId)) } returns buildVariantDto()

        val request = mapOf(
            "name" to "Standard",
            "durationMinutes" to 30,
            "price" to 50.00,
        )

        mockMvc.post("/api/services/${UUID.randomUUID()}/variants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createVariant duration not multiple of 5 returns 400`() {
        every { serviceService.createVariant(any(), any(), eq(salonId)) } throws InvalidDurationException(17)

        val request = mapOf(
            "name" to "Bad",
            "durationMinutes" to 17,
            "price" to 50.00,
        )

        mockMvc.post("/api/services/${UUID.randomUUID()}/variants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createVariant negative price returns 400`() {
        val request = mapOf(
            "name" to "Bad",
            "durationMinutes" to 30,
            "price" to -1.00,
        )

        mockMvc.post("/api/services/${UUID.randomUUID()}/variants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `updateVariant returns 200`() {
        every { serviceService.updateVariant(any(), any(), eq(salonId)) } returns buildVariantDto()

        val request = mapOf("name" to "Premium")

        mockMvc.put("/api/services/${UUID.randomUUID()}/variants/${UUID.randomUUID()}") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `deactivateVariant returns 204`() {
        mockMvc.delete("/api/services/${UUID.randomUUID()}/variants/${UUID.randomUUID()}")
            .andExpect {
                status { isNoContent() }
            }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `reorderServices valid returns 204`() {
        val request = mapOf("orderedIds" to listOf(UUID.randomUUID(), UUID.randomUUID()))

        mockMvc.post("/api/services/reorder") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `reorderServices empty list returns 400`() {
        val request = mapOf("orderedIds" to emptyList<UUID>())

        mockMvc.post("/api/services/reorder") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createCategory valid returns 201`() {
        every { serviceService.createCategory(any(), eq(salonId)) } returns buildCategoryDto()

        val request = mapOf("name" to "Włosy", "colorHex" to "#FF0000")

        mockMvc.post("/api/services/categories") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `createCategory duplicate returns 409`() {
        every { serviceService.createCategory(any(), eq(salonId)) } throws
            DuplicateCategoryNameException("Włosy", salonId)

        val request = mapOf("name" to "Włosy")

        mockMvc.post("/api/services/categories") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @WithMockUser(roles = ["MANAGER"])
    fun `deleteCategory returns 204`() {
        mockMvc.delete("/api/services/categories/${UUID.randomUUID()}")
            .andExpect {
                status { isNoContent() }
            }
    }

    @Test
    @WithMockUser(roles = ["EMPLOYEE"])
    fun `employeeRole createService returns 403`() {
        val request = mapOf("name" to "Test", "category" to "Cat")

        mockMvc.post("/api/services") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }
    }
}

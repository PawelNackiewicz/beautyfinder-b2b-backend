package com.beautyfinder.b2b.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

data class HealthResponse(
    val status: String,
    val timestamp: OffsetDateTime,
    val version: String,
)

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check endpoints")
class HealthController {

    @GetMapping
    @Operation(summary = "Health check", description = "Returns application health status")
    fun health(): HealthResponse = HealthResponse(
        status = "UP",
        timestamp = OffsetDateTime.now(),
        version = "1.0.0",
    )
}

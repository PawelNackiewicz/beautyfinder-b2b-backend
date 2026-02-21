package com.beautyfinder.b2b.api

import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

data class ErrorResponse(
    val timestamp: OffsetDateTime,
    val status: Int,
    val message: String,
    val path: String,
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleEntityNotFound(ex: EntityNotFoundException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 404,
            message = ex.message ?: "Entity not found",
            path = request.requestURI,
        )

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDenied(ex: AccessDeniedException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 403,
            message = ex.message ?: "Access denied",
            path = request.requestURI,
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ErrorResponse {
        val message = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = message,
            path = request.requestURI,
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ErrorResponse =
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = 400,
            message = ex.message ?: "Bad request",
            path = request.requestURI,
        )
}

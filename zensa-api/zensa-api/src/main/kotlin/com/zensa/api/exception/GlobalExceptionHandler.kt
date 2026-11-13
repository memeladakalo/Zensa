package com.zensa.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Converts exceptions into clean JSON error responses.
 *
 * The handler inspects the exception message to decide the HTTP
 * status code:
 *   "not found"                 → 404
 *   "Incorrect email/password"  → 401
 *   anything else               → 409 (business rule conflict)
 *
 * This is why the Android app never sees a raw stack trace.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf(
                "status" to 400,
                "message" to "Validation failed",
                "errors" to errors
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<Map<String, Any>> {
        val message = ex.message ?: "Request could not be processed"

        val status = when {
            message.contains("not found", ignoreCase = true) ->
                HttpStatus.NOT_FOUND

            message.contains("Incorrect", ignoreCase = true) ->
                HttpStatus.UNAUTHORIZED

            else ->
                HttpStatus.CONFLICT
        }

        return ResponseEntity.status(status).body(
            mapOf("status" to status.value(), "message" to message)
        )
    }

    /** Optional cloud service not configured / unreachable -> 503. */
    @ExceptionHandler(ServiceUnavailableException::class)
    fun handleUnavailable(ex: ServiceUnavailableException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            mapOf("status" to 503, "message" to (ex.message ?: "Service unavailable"))
        )

    /** Rejected upload (type / size / content) -> 400, 413 or 415 as chosen by FileRules. */
    @ExceptionHandler(FileRejectedException::class)
    fun handleFileRejected(ex: FileRejectedException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(ex.status).body(
            mapOf("status" to ex.status.value(), "message" to (ex.message ?: "File rejected"))
        )

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleTooLarge(ex: MaxUploadSizeExceededException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            mapOf("status" to 413, "message" to "File is too large (max 10 MB)")
        )

    @ExceptionHandler(
        MissingServletRequestPartException::class,
        MissingServletRequestParameterException::class
    )
    fun handleMissingPart(ex: Exception): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf("status" to 400, "message" to "A required value is missing")
        )

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "status" to 500,
                "message" to "Something went wrong. Please try again later."
            )
        )
    }
}

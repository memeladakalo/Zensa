package com.zensa.api.exception

import org.springframework.http.HttpStatus

/**
 * Thrown when an optional cloud service (Firebase, Azure Blob Storage, Azure Cosmos DB,
 * Google SSO) has not been configured with credentials, or is temporarily unreachable.
 * The GlobalExceptionHandler turns it into HTTP 503 with a friendly message.
 */
class ServiceUnavailableException(message: String) : RuntimeException(message)

/** Thrown when an uploaded file is rejected (wrong type, too big, empty, spoofed content). */
class FileRejectedException(
    message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)

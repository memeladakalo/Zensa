package com.zensa.api.service.storage

import com.zensa.api.exception.FileRejectedException
import org.springframework.http.HttpStatus
import java.util.UUID

/** An upload that passed every check. */
data class ValidatedFile(val safeName: String, val contentType: String, val bytes: ByteArray)

/**
 * FileRules - validation for uploaded files (pure, unit-tested).
 *   - size limit (10 MB) and non-empty
 *   - extension allow-list (pdf, png, jpg/jpeg, txt, docx)
 *   - the content type is derived from the extension, NOT trusted from the client
 *   - the first bytes must match the claimed type ("magic numbers"), so a renamed .exe is rejected
 *   - file names are sanitised (no paths, no odd characters)
 */
object FileRules {
    const val MAX_BYTES: Long = 10L * 1024 * 1024

    private val TYPES = mapOf(
        "pdf" to "application/pdf",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "txt" to "text/plain",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )

    fun sanitizeName(raw: String?): String {
        val base = raw.orEmpty().substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = base.replace(Regex("[^A-Za-z0-9._ -]"), "_").replace(Regex("\\.{2,}"), ".")
        return cleaned.takeLast(100).ifBlank { "attachment" }
    }

    fun extensionOf(name: String): String = name.substringAfterLast('.', "").lowercase()

    fun validate(fileName: String?, bytes: ByteArray): ValidatedFile {
        if (bytes.isEmpty()) throw FileRejectedException("The file is empty")
        if (bytes.size > MAX_BYTES) {
            throw FileRejectedException("File is too large (max 10 MB)", HttpStatus.PAYLOAD_TOO_LARGE)
        }
        val safe = sanitizeName(fileName)
        val type = TYPES[extensionOf(safe)] ?: throw FileRejectedException(
            "This file type is not allowed (allowed: pdf, png, jpg, txt, docx)",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE
        )
        if (!contentMatches(extensionOf(safe), bytes)) {
            throw FileRejectedException(
                "The file content does not match its type", HttpStatus.UNSUPPORTED_MEDIA_TYPE
            )
        }
        return ValidatedFile(safe, type, bytes)
    }

    private fun startsWith(bytes: ByteArray, vararg prefix: Int): Boolean =
        bytes.size >= prefix.size && prefix.indices.all { (bytes[it].toInt() and 0xFF) == prefix[it] }

    private fun contentMatches(ext: String, bytes: ByteArray): Boolean = when (ext) {
        "pdf" -> startsWith(bytes, 0x25, 0x50, 0x44, 0x46)            // %PDF
        "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47)
        "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF)
        "docx" -> startsWith(bytes, 0x50, 0x4B, 0x03, 0x04)           // zip container "PK.."
        "txt" -> bytes.take(1024).none { it.toInt() == 0 }             // plain text has no NUL bytes
        else -> false
    }

    /** Blob names are "<userId>/<uuid>-<name>": one folder per user. */
    fun newBlobName(userId: UUID, safeName: String): String = "$userId/${UUID.randomUUID()}-$safeName"

    /** Ownership check used for download links and deletes. */
    fun isOwnedBy(userId: UUID, blobName: String): Boolean =
        blobName.startsWith("$userId/") &&
            blobName.length < 300 &&
            !blobName.contains("..") &&
            !blobName.contains('\\') &&
            blobName.count { it == '/' } == 1
}

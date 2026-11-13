package com.zensa.api.service.storage

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobHttpHeaders
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.zensa.api.exception.ServiceUnavailableException
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

data class StoredFile(val blobName: String, val fileName: String, val sizeBytes: Long, val contentType: String)

/** Per-user file storage. All methods receive the OWNER's id, taken from the verified JWT. */
interface BlobStorageService {
    fun upload(userId: UUID, file: ValidatedFile): StoredFile

    /** Short-lived, read-only download URL. @throws IllegalArgumentException("... not found") when not the owner's file. */
    fun downloadLink(userId: UUID, blobName: String, minutes: Int = 10): String

    fun delete(userId: UUID, blobName: String): Boolean
}

/**
 * Azure Blob Storage implementation. Credentials come ONLY from the environment
 * (AZURE_STORAGE_CONNECTION_STRING); the Android app never sees them. The container is
 * private: files are only reachable through short-lived SAS links created after an
 * ownership check.
 */
class AzureBlobStorageService(
    private val connectionString: String,
    private val containerName: String
) : BlobStorageService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val container: BlobContainerClient by lazy {
        try {
            BlobServiceClientBuilder().connectionString(connectionString).buildClient()
                .getBlobContainerClient(containerName)
                .also { it.createIfNotExists() }
        } catch (e: Exception) {
            log.error("Azure Blob Storage unavailable: {}", e.javaClass.simpleName)
            throw ServiceUnavailableException("File storage is temporarily unavailable")
        }
    }

    override fun upload(userId: UUID, file: ValidatedFile): StoredFile {
        val name = FileRules.newBlobName(userId, file.safeName)
        try {
            val blob = container.getBlobClient(name)
            blob.upload(BinaryData.fromBytes(file.bytes), true)
            blob.setHttpHeaders(BlobHttpHeaders().setContentType(file.contentType))
        } catch (e: ServiceUnavailableException) {
            throw e
        } catch (e: Exception) {
            log.error("Blob upload failed: {}", e.javaClass.simpleName)
            throw ServiceUnavailableException("File storage is temporarily unavailable")
        }
        return StoredFile(name, file.safeName, file.bytes.size.toLong(), file.contentType)
    }

    override fun downloadLink(userId: UUID, blobName: String, minutes: Int): String {
        if (!FileRules.isOwnedBy(userId, blobName)) throw IllegalArgumentException("File not found")
        val blob = container.getBlobClient(blobName)
        if (!blob.exists()) throw IllegalArgumentException("File not found")
        val sas = blob.generateSas(
            BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(minutes.toLong()),
                BlobSasPermission().setReadPermission(true)
            )
        )
        return "${blob.blobUrl}?$sas"
    }

    override fun delete(userId: UUID, blobName: String): Boolean {
        if (!FileRules.isOwnedBy(userId, blobName)) throw IllegalArgumentException("File not found")
        return container.getBlobClient(blobName).deleteIfExists()
    }
}

/** Used when no storage connection string has been configured. */
class UnconfiguredBlobStorageService : BlobStorageService {
    private fun fail(): Nothing =
        throw ServiceUnavailableException("File storage is not configured on the server")

    override fun upload(userId: UUID, file: ValidatedFile): StoredFile = fail()
    override fun downloadLink(userId: UUID, blobName: String, minutes: Int): String = fail()
    override fun delete(userId: UUID, blobName: String): Boolean = fail()
}

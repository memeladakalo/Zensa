package com.zensa.api.cloud

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * Azure Blob Storage integration for note attachments / module media.
 * When [connectionString] is blank, uploads are no-ops (local dev).
 * Wire real Azure SDK (com.azure:azure-storage-blob) for production.
 */
@Service
class AzureBlobService(
    @Value("\${azure.storage.connection-string:}") private val connectionString: String,
    @Value("\${azure.storage.container:zensa-uploads}") private val container: String
) {
    fun isConfigured(): Boolean = connectionString.isNotBlank()

    fun upload(file: MultipartFile, path: String): String {
        if (!isConfigured()) {
            return "local://$path/${file.originalFilename}"
        }
        // Production: BlobClient.upload(...); return blob URL
        return "https://blob.core.windows.net/$container/$path/${file.originalFilename}"
    }
}

package com.zensa.api.controller

import com.zensa.api.dto.FileLinkResponse
import com.zensa.api.dto.FileUploadResponse
import com.zensa.api.service.CurrentUser
import com.zensa.api.service.storage.BlobStorageService
import com.zensa.api.service.storage.FileRules
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Note attachments stored in Azure Blob Storage.
 * The owner is ALWAYS the authenticated user: blob names are "<userId>/...", and every
 * link/delete request is checked against that prefix, so users cannot reach each other's files.
 */
@RestController
@RequestMapping("/api/files")
class FileController(private val storage: BlobStorageService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<FileUploadResponse> {
        val userId = CurrentUser.id() ?: return ResponseEntity.status(401).build()
        val validated = FileRules.validate(file.originalFilename, file.bytes)
        val stored = storage.upload(userId, validated)
        return ResponseEntity.ok(
            FileUploadResponse(stored.blobName, stored.fileName, stored.sizeBytes, stored.contentType)
        )
    }

    /** A 10-minute read-only link for opening the file. */
    @GetMapping("/link")
    fun link(@RequestParam("name") name: String): ResponseEntity<FileLinkResponse> {
        val userId = CurrentUser.id() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(FileLinkResponse(storage.downloadLink(userId, name, LINK_MINUTES), LINK_MINUTES))
    }

    @DeleteMapping
    fun delete(@RequestParam("name") name: String): ResponseEntity<Void> {
        val userId = CurrentUser.id() ?: return ResponseEntity.status(401).build()
        storage.delete(userId, name)
        return ResponseEntity.noContent().build()
    }

    private companion object {
        const val LINK_MINUTES = 10
    }
}

package com.zensa.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.FileUploadResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * FileRepository - uploads a note attachment to Azure Blob Storage through the API and
 * asks the API for a short-lived download link.
 *
 * The Android app NEVER holds Azure credentials: it only sends the file to the Zensa API
 * (authenticated with the user's JWT). The API validates type/size, stores the blob under
 * the user's own folder, and later returns a read-only link that expires after minutes.
 */
class FileRepository(
    private val api: ZensaApiService,
    private val contentResolver: ContentResolver
) {

    /** Reads the picked document (SAF uri), validates its size locally, and uploads it. */
    suspend fun upload(uri: Uri): ApiResult<FileUploadResponseDto> = withContext(Dispatchers.IO) {
        try {
            val (name, size) = describe(uri)
            if (size > MAX_BYTES) {
                return@withContext ApiResult.Error("File is too large (max 10 MB)")
            }
            val mime = contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext ApiResult.Error("Could not read the selected file")
            if (bytes.size > MAX_BYTES) {
                return@withContext ApiResult.Error("File is too large (max 10 MB)")
            }

            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", name, body)
            val response = api.uploadFile(part)
            val result = response.body()
            if (response.isSuccessful && result != null) {
                Log.i(TAG, "Uploaded attachment (${bytes.size} bytes)")
                ApiResult.Success(result)
            } else {
                ApiResult.Error(
                    when (response.code()) {
                        400, 413, 415 -> "This file type or size is not allowed"
                        401 -> "Incorrect email or password"
                        503 -> "File storage is not configured on the server"
                        else -> "Upload failed (${response.code()})"
                    }
                )
            }
        } catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    /** Short-lived read-only link for opening an attachment. */
    suspend fun downloadLink(blobName: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.fileLink(blobName)
            val body = response.body()
            if (response.isSuccessful && body != null) ApiResult.Success(body.url)
            else ApiResult.Error("Could not open the attachment (${response.code()})")
        } catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    /** Deletes a blob the user owns (the API rejects names outside the user's own folder). */
    suspend fun delete(blobName: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteFile(blobName)
            if (response.isSuccessful) ApiResult.Success(Unit)
            else ApiResult.Error("Could not delete the attachment (${response.code()})")
        } catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        }
    }

    private fun describe(uri: Uri): Pair<String, Long> {
        var name = "attachment"
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val s = c.getColumnIndex(OpenableColumns.SIZE)
                if (n >= 0) name = c.getString(n) ?: name
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
            }
        }
        return name to size
    }

    companion object {
        private const val TAG = "FileRepository"
        const val MAX_BYTES = 10L * 1024 * 1024

        /** The label to show for a stored blob name like "<userId>/<uuid>-notes.pdf". */
        fun displayName(blobName: String): String =
            blobName.substringAfterLast('/').let { last ->
                // strip the generated "<uuid>-" prefix (36 chars + dash)
                if (last.length > 37 && last[36] == '-') last.substring(37) else last
            }
    }
}

package com.zensa.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** POST /api/auth/google */
data class GoogleLoginRequest(
    @field:NotBlank(message = "idToken is required")
    @field:Size(max = 8192, message = "idToken is too long")
    val idToken: String = ""
)

/** POST/DELETE /api/devices */
data class DeviceTokenRequest(
    @field:NotBlank(message = "token is required")
    @field:Size(max = 512, message = "token is too long")
    val token: String = "",
    @field:Size(max = 20)
    val platform: String = "android"
)

data class TestNotificationResponse(val sent: Int, val message: String)

data class FileUploadResponse(
    val blobName: String,
    val fileName: String,
    val sizeBytes: Long,
    val contentType: String
)

data class FileLinkResponse(val url: String, val expiresInMinutes: Int)

/** PUT /api/gamification/me */
data class GamificationRequest(
    @field:Min(0) @field:Max(10_000_000) val points: Int = 0,
    @field:Min(1) @field:Max(10_000) val level: Int = 1,
    @field:Min(0) @field:Max(100_000) val currentStreakDays: Int = 0,
    @field:Min(0) @field:Max(100_000) val longestStreakDays: Int = 0,
    @field:Size(max = 100) val unlockedBadges: List<String> = emptyList()
)

data class GamificationResponse(
    val userId: String,
    val points: Int,
    val level: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val unlockedBadges: List<String>,
    val updatedAt: String
)

package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Part 3 DTOs (SSO, push notifications, Blob Storage, Cosmos DB gamification)
// ---------------------------------------------------------------------------

/** POST /api/auth/google - the Google ID token obtained through OAuth 2.0 + PKCE. */
@Serializable
data class GoogleLoginRequestDto(val idToken: String)

/** POST /api/devices - registers this phone's Firebase Cloud Messaging token. */
@Serializable
data class DeviceTokenRequestDto(
    val token: String,
    val platform: String = "android"
)

/** Result of POST /api/notifications/test. */
@Serializable
data class TestNotificationResultDto(
    val sent: Int = 0,
    val message: String = ""
)

/** Result of POST /api/files (Azure Blob Storage upload). */
@Serializable
data class FileUploadResponseDto(
    val blobName: String,
    val fileName: String,
    val sizeBytes: Long = 0,
    val contentType: String = ""
)

/** Result of GET /api/files/link: a short-lived, read-only download URL (SAS). */
@Serializable
data class FileLinkDto(
    val url: String,
    val expiresInMinutes: Int = 10
)

/** Body of PUT /api/gamification/me (stored as a Cosmos DB document). */
@Serializable
data class GamificationProfileRequestDto(
    val points: Int,
    val level: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val unlockedBadges: List<String>
)

/** Document returned by GET/PUT /api/gamification/me. */
@Serializable
data class GamificationProfileDto(
    val userId: String = "",
    val points: Int = 0,
    val level: Int = 1,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val unlockedBadges: List<String> = emptyList(),
    val updatedAt: String? = null
)

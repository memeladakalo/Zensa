package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.GamificationProfileDto
import com.zensa.app.data.remote.dto.GamificationProfileRequestDto
import com.zensa.app.domain.model.AchievementSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * GamificationCloudRepository - stores and reads the student's gamification progress in the
 * cloud NoSQL database (Azure Cosmos DB) through the Spring Boot API.
 *
 *   save()  -> PUT /api/gamification/me   (create/update the user's document)
 *   load()  -> GET /api/gamification/me   (read it back, e.g. on a new phone)
 *
 * Room stays the source of truth on the device; this is a cloud backup / restore.
 * Failures (offline, cloud not configured) are returned as ApiResult.Error and are
 * never fatal.
 */
class GamificationCloudRepository(private val api: ZensaApiService) {

    suspend fun save(snapshot: AchievementSnapshot): ApiResult<GamificationProfileDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.saveGamification(
                    GamificationProfileRequestDto(
                        points = snapshot.points,
                        level = snapshot.level.level,
                        currentStreakDays = snapshot.currentStreakDays,
                        longestStreakDays = snapshot.longestStreakDays,
                        unlockedBadges = snapshot.achievements
                            .filter { it.unlocked }
                            .map { it.achievement.id }
                    )
                )
                val body = response.body()
                if (response.isSuccessful && body != null) ApiResult.Success(body)
                else ApiResult.Error("Cloud backup unavailable (${response.code()})")
            } catch (e: IOException) {
                ApiResult.Error("Offline — cloud backup skipped")
            } catch (e: Exception) {
                ApiResult.Error("Unexpected error: ${e.message}")
            }
        }

    /** Returns the stored document, or Success(null) when nothing has been stored yet (HTTP 404). */
    suspend fun load(): ApiResult<GamificationProfileDto?> = withContext(Dispatchers.IO) {
        try {
            val response = api.getGamification()
            when {
                response.isSuccessful -> ApiResult.Success(response.body())
                response.code() == 404 -> ApiResult.Success(null)
                else -> ApiResult.Error("Cloud read unavailable (${response.code()})")
            }
        } catch (e: IOException) {
            ApiResult.Error("Offline — cloud read skipped")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }
}

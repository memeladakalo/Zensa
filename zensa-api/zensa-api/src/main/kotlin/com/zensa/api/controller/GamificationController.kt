package com.zensa.api.controller

import com.zensa.api.dto.GamificationRequest
import com.zensa.api.dto.GamificationResponse
import com.zensa.api.service.CurrentUser
import com.zensa.api.service.cosmos.GamificationProfile
import com.zensa.api.service.cosmos.GamificationStore
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * The student's gamification progress, stored in Azure Cosmos DB (NoSQL).
 *   PUT /api/gamification/me  -> create / update the document   (NoSQL write)
 *   GET /api/gamification/me  -> read it back                    (NoSQL read)
 * "me" is always the authenticated user; there is no way to address another user's document.
 */
@RestController
@RequestMapping("/api/gamification")
class GamificationController(private val store: GamificationStore) {

    @PutMapping("/me")
    fun save(@Valid @RequestBody request: GamificationRequest): ResponseEntity<GamificationResponse> {
        val userId = CurrentUser.id() ?: return ResponseEntity.status(401).build()
        val saved = store.save(
            GamificationProfile(
                userId = userId.toString(),
                points = request.points,
                level = request.level,
                currentStreakDays = request.currentStreakDays,
                // The best streak can never be shorter than the current one.
                longestStreakDays = maxOf(request.longestStreakDays, request.currentStreakDays),
                unlockedBadges = request.unlockedBadges.map { it.take(MAX_BADGE_ID) }.distinct(),
                updatedAt = LocalDateTime.now().toString()
            )
        )
        return ResponseEntity.ok(saved.toResponse())
    }

    @GetMapping("/me")
    fun load(): ResponseEntity<GamificationResponse> {
        val userId = CurrentUser.id() ?: return ResponseEntity.status(401).build()
        val profile = store.find(userId.toString()) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(profile.toResponse())
    }

    private fun GamificationProfile.toResponse() = GamificationResponse(
        userId, points, level, currentStreakDays, longestStreakDays, unlockedBadges, updatedAt
    )

    private companion object {
        const val MAX_BADGE_ID = 80
    }
}

package com.zensa.api.service.cosmos

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The Cosmos DB document shape: id = partition key = userId, and a lossless round trip. */
class GamificationDocumentsTest {

    private val profile = GamificationProfile(
        userId = "u-1", points = 120, level = 2, currentStreakDays = 3,
        longestStreakDays = 5, unlockedBadges = listOf("a", "b"), updatedAt = "2026-11-07T10:00:00"
    )

    @Test
    fun `document uses the user id as id and partition key`() {
        val doc = GamificationDocuments.toDocument(profile)
        assertEquals("u-1", doc["id"])
        assertEquals("u-1", doc["userId"])
        assertEquals(GamificationDocuments.TYPE, doc["type"])
    }

    @Test
    fun `round trip keeps every field`() {
        val back = GamificationDocuments.fromDocument(GamificationDocuments.toDocument(profile))
        assertEquals(profile, back)
    }

    @Test
    fun `missing fields fall back to safe defaults`() {
        val back = GamificationDocuments.fromDocument(mapOf("id" to "u-9"))
        assertEquals("u-9", back.userId)
        assertEquals(0, back.points)
        assertEquals(1, back.level)
        assertTrue(back.unlockedBadges.isEmpty())
    }

    @Test
    fun `numbers returned as long or double are converted`() {
        val back = GamificationDocuments.fromDocument(
            mapOf("userId" to "u", "points" to 15L, "level" to 2.0, "unlockedBadges" to listOf("x"))
        )
        assertEquals(15, back.points)
        assertEquals(2, back.level)
        assertEquals(listOf("x"), back.unlockedBadges)
    }
}

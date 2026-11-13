package com.zensa.app

import com.zensa.app.domain.model.LevelInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the Gamification level rules (User Defined 5). Pure JVM, no Android needed. */
class LevelInfoTest {

    @Test
    fun `zero points is level 1 with starter title`() {
        val info = LevelInfo.fromPoints(0)
        assertEquals(1, info.level)
        assertEquals(LevelInfo.TITLE_STARTER, info.titleKey)
        assertEquals(0, info.pointsIntoLevel)
    }

    @Test
    fun `every hundred points is a new level`() {
        assertEquals(1, LevelInfo.fromPoints(99).level)
        assertEquals(2, LevelInfo.fromPoints(100).level)
        assertEquals(3, LevelInfo.fromPoints(250).level)
    }

    @Test
    fun `progress is a fraction of the current level`() {
        val info = LevelInfo.fromPoints(150)
        assertEquals(50, info.pointsIntoLevel)
        assertEquals(0.5f, info.progress, 0.0001f)
    }

    @Test
    fun `high levels use the zen title`() {
        assertEquals(LevelInfo.TITLE_ZEN, LevelInfo.fromPoints(900).titleKey)
    }

    @Test
    fun `negative points never produce a level below 1`() {
        val info = LevelInfo.fromPoints(-50)
        assertEquals(1, info.level)
        assertTrue(info.progress >= 0f)
    }
}

package com.zensa.app.domain.gamification

/**
 * Part 3 gamification: XP, levels, streaks, daily goals.
 */
data class UserProgress(
    val totalXp: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0,
    val dailyGoalMinutes: Int = 25,
    val todayMinutes: Int = 0
) {
    val xpForCurrentLevel: Int get() = (level - 1) * 100
    val xpForNextLevel: Int get() = level * 100
    val xpProgressInLevel: Int get() = (totalXp - xpForCurrentLevel).coerceAtLeast(0)
    val xpNeededForNext: Int get() = (xpForNextLevel - totalXp).coerceAtLeast(0)
    val dailyGoalReached: Boolean get() = todayMinutes >= dailyGoalMinutes
}

enum class XpEvent(val xp: Int) {
    STUDY_SESSION_COMPLETE(20),
    ASSIGNMENT_COMPLETE(50),
    NOTE_CREATED(5),
    STREAK_DAY(15),
    DAILY_GOAL(30),
    BADGE_UNLOCK(40)
}

object GamificationRules {
    fun levelForXp(xp: Int): Int = (xp / 100) + 1
}

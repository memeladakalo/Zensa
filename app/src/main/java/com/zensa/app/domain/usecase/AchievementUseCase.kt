package com.zensa.app.domain.usecase

import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.StudySessionEntity
import com.zensa.app.data.entity.SubtaskEntity
import com.zensa.app.domain.model.Achievement
import com.zensa.app.domain.model.AchievementSnapshot
import com.zensa.app.domain.model.UnlockedAchievement
import java.time.LocalDate

/**
 * AchievementsUseCase
 *
 * Pure Kotlin. No Android. No Room. No Retrofit.
 *
 * Given the user's assignments, subtasks, and study sessions, it:
 *   1. Computes the metrics each badge depends on.
 *   2. Determines which badges are unlocked.
 *   3. Computes a total points score.
 *   4. Returns a single AchievementSnapshot for the UI.
 *
 * `today` and `unlockedAtMap` are injected so the result is
 * deterministic and testable:
 *   - today            : fixes the streak calculation
 *   - unlockedAtMap    : the dates when badges were unlocked
 *                        (persisted in Room); missing entries are
 *                        treated as "unlocked but date unknown".
 *
 * Points weighting (kept simple and explainable):
 *   - 10 points per completed assignment
 *   - 2  points per completed subtask
 *   - 1  point per completed minute of study
 *   - 5  bonus points for every extra day in the longest study streak
 *        (a 1-day "streak" earns nothing, a 3-day streak earns 10)
 *
 * Badges are PERMANENT: once a badge has an unlock record (unlockedAtMap)
 * it stays unlocked even if the live metric later drops (e.g. a streak
 * is broken or an assignment is un-completed). Streak badges are gated on
 * the LONGEST streak, so a streak achieved between two visits is never missed.
 */
class AchievementsUseCase(
    private val studyStats: StudySessionStatsUseCase = StudySessionStatsUseCase()
) {

    fun compute(
        assignments: List<AssignmentEntity>,
        subtasks: List<SubtaskEntity>,
        sessions: List<StudySessionEntity>,
        unlockedAtMap: Map<String, String> = emptyMap(),
        today: LocalDate = LocalDate.now()
    ): AchievementSnapshot {

        // -------- Metrics --------

        val liveAssignments = assignments.filter { !it.isDeleted }
        val liveSubtasks = subtasks.filter { !it.isDeleted }
        val liveSessions = sessions.filter { !it.isDeleted && it.completed }

        val completedAssignments = liveAssignments.count { it.isCompleted }
        val completedSubtasks = liveSubtasks.count { it.isCompleted }
        val totalStudyMinutes =
            liveSessions.sumOf { it.durationSeconds } / 60

        val stats = studyStats.summarise(liveSessions, today)
        val completedSessions = liveSessions.size

        // -------- Points --------

        val streakBonus = (stats.longestStreakDays - 1).coerceAtLeast(0) * POINTS_PER_STREAK_DAY

        val points =
            completedAssignments * POINTS_PER_ASSIGNMENT +
            completedSubtasks * POINTS_PER_SUBTASK +
            totalStudyMinutes * POINTS_PER_STUDY_MINUTE +
            streakBonus

        // -------- Unlock decisions --------

        val unlocked = Achievement.entries.map { achievement ->
            val value = metricFor(
                achievement = achievement,
                completedAssignments = completedAssignments,
                completedSubtasks = completedSubtasks,
                totalStudyMinutes = totalStudyMinutes,
                completedSessions = completedSessions,
                bestStreak = stats.longestStreakDays
            )
            // Permanent: unlocked now OR already recorded as unlocked earlier.
            val isUnlocked = value >= achievement.threshold ||
                achievement.id in unlockedAtMap
            UnlockedAchievement(
                achievement = achievement,
                unlocked = isUnlocked,
                unlockedAt = if (isUnlocked) unlockedAtMap[achievement.id] else null
            )
        }

        return AchievementSnapshot(
            points = points,
            currentStreakDays = stats.currentStreakDays,
            longestStreakDays = stats.longestStreakDays,
            totalStudyMinutes = totalStudyMinutes,
            completedAssignments = completedAssignments,
            completedSubtasks = completedSubtasks,
            achievements = unlocked
        )
    }

    /**
     * Returns the metric value that the given badge is gated on.
     * Keeping this in one place makes it trivial to add a new badge
     * later: add the enum entry and one branch here.
     */
    internal fun metricFor(
        achievement: Achievement,
        completedAssignments: Int,
        completedSubtasks: Int,
        totalStudyMinutes: Int,
        completedSessions: Int,
        bestStreak: Int
    ): Int = when (achievement) {
        // "First session" counts sessions, not minutes (a 30-second session still counts).
        Achievement.FIRST_STUDY_SESSION ->
            completedSessions

        Achievement.STUDIED_60_MINUTES,
        Achievement.STUDIED_600_MINUTES ->
            totalStudyMinutes

        Achievement.STREAK_3_DAYS,
        Achievement.STREAK_7_DAYS,
        Achievement.STREAK_30_DAYS ->
            bestStreak

        Achievement.FIRST_ASSIGNMENT_COMPLETED,
        Achievement.FIVE_ASSIGNMENTS_COMPLETED,
        Achievement.TWENTY_ASSIGNMENTS_COMPLETED ->
            completedAssignments

        Achievement.TEN_SUBTASKS_COMPLETED,
        Achievement.FIFTY_SUBTASKS_COMPLETED ->
            completedSubtasks
    }

    companion object {
        const val POINTS_PER_ASSIGNMENT = 10
        const val POINTS_PER_SUBTASK = 2
        const val POINTS_PER_STUDY_MINUTE = 1
        const val POINTS_PER_STREAK_DAY = 5
    }
}

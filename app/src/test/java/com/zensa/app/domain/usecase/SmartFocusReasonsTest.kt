package com.zensa.app.domain.usecase

import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.SubtaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Tests for the "why is this in my focus list?" explanations (User Defined 4: Smart Focus). */
class SmartFocusReasonsTest {

    private val today = LocalDate.of(2026, 6, 15)
    private val useCase = SmartFocusUseCase()

    private fun assignment(
        id: String = "a",
        due: String = "2026-06-30T23:59:00",
        priority: String = "MEDIUM",
        status: String = "PENDING"
    ) = AssignmentEntity(
        localId = id, title = "T", dueDate = due, priority = priority, status = status,
        isCompleted = false, createdAt = "2026-06-01T00:00:00", updatedAt = "2026-06-01T00:00:00"
    )

    private fun subtask(done: Boolean) = SubtaskEntity(
        localId = "s${System.nanoTime()}", assignmentLocalId = "a", title = "x", position = 0,
        isCompleted = done, createdAt = "2026-06-01T00:00:00", updatedAt = "2026-06-01T00:00:00"
    )

    @Test
    fun `overdue urgent assignment explains both reasons`() {
        val result = useCase.rank(
            listOf(assignment(due = "2026-06-10T00:00:00", priority = "URGENT")), today = today
        )
        assertEquals(
            listOf(SmartFocusUseCase.REASON_OVERDUE, SmartFocusUseCase.REASON_URGENT),
            result.first().reasons
        )
    }

    @Test
    fun `reasons never exceed the maximum`() {
        val result = useCase.rank(
            listOf(assignment(due = "2026-06-15T00:00:00", priority = "HIGH", status = "IN_PROGRESS")),
            today = today
        )
        assertTrue(result.first().reasons.size <= SmartFocusUseCase.MAX_REASONS)
    }

    @Test
    fun `partly done subtasks are reported`() {
        val a = assignment(id = "a")
        val result = useCase.rank(
            listOf(a),
            subtasksByAssignment = mapOf("a" to listOf(subtask(true), subtask(false))),
            today = today
        )
        assertTrue(SmartFocusUseCase.REASON_PARTLY_DONE in result.first().reasons)
    }

    @Test
    fun `far away low priority assignment has no reasons`() {
        val result = useCase.rank(listOf(assignment(priority = "LOW")), today = today)
        assertTrue(result.first().reasons.isEmpty())
    }

    @Test
    fun `reasons do not change the score or the ranking`() {
        val urgent = assignment(id = "u", due = "2026-06-16T00:00:00", priority = "URGENT")
        val later = assignment(id = "l", due = "2026-07-30T00:00:00", priority = "LOW")
        val ranked = useCase.rank(listOf(later, urgent), today = today)
        assertEquals("u", ranked.first().assignment.localId)
        assertEquals(useCase.score(urgent, emptyList(), today), ranked.first().score)
    }
}

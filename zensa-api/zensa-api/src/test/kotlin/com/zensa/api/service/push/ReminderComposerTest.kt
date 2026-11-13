package com.zensa.api.service.push

import com.zensa.api.entity.AssignmentEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/** Pure logic behind the daily deadline push notifications. */
class ReminderComposerTest {

    private val now = LocalDateTime.of(2026, 11, 7, 7, 0)
    private val user = UUID.randomUUID()

    private fun a(title: String, due: String, done: Boolean = false, owner: UUID? = user) =
        AssignmentEntity(userId = owner, title = title, dueDate = due, isCompleted = done)

    @Test
    fun `parses date-time and date-only values`() {
        assertEquals(LocalDateTime.of(2026, 11, 8, 10, 0), ReminderComposer.parseDue("2026-11-08T10:00:00"))
        assertEquals(LocalDateTime.of(2026, 11, 8, 23, 59), ReminderComposer.parseDue("2026-11-08"))
        assertNull(ReminderComposer.parseDue("not a date"))
    }

    @Test
    fun `only incomplete assignments due within 24 hours are selected`() {
        val items = ReminderComposer.dueSoon(
            listOf(
                a("tomorrow morning", "2026-11-08T06:00:00"),
                a("already done", "2026-11-08T06:00:00", done = true),
                a("next week", "2026-11-14T06:00:00"),
                a("overdue", "2026-11-01T06:00:00")
            ),
            now
        )
        assertEquals(listOf("tomorrow morning"), items.map { it.title })
    }

    @Test
    fun `assignments without an owner are ignored`() {
        assertTrue(ReminderComposer.dueSoon(listOf(a("x", "2026-11-07T20:00:00", owner = null)), now).isEmpty())
    }

    @Test
    fun `one item gives a singular message`() {
        val (title, body) = ReminderComposer.compose(
            ReminderComposer.dueSoon(listOf(a("DB Design", "2026-11-07T20:00:00")), now)
        )
        assertEquals("Assignment due soon", title)
        assertTrue(body.contains("DB Design"))
    }

    @Test
    fun `many items are summarised`() {
        val items = ReminderComposer.dueSoon(
            listOf(1, 2, 3, 4, 5).map { a("T$it", "2026-11-07T${10 + it}:00:00") }, now
        )
        val (title, body) = ReminderComposer.compose(items)
        assertEquals("5 assignments due soon", title)
        assertTrue(body.contains("and 2 more"))
    }

    @Test
    fun `items are grouped per user`() {
        val other = UUID.randomUUID()
        val grouped = ReminderComposer.byUser(
            ReminderComposer.dueSoon(
                listOf(a("a", "2026-11-07T10:00:00"), a("b", "2026-11-07T11:00:00", owner = other)), now
            )
        )
        assertEquals(setOf(user, other), grouped.keys)
    }
}

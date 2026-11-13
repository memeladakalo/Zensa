package com.zensa.api.service.push

import com.zensa.api.entity.AssignmentEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

data class DueItem(val userId: UUID, val title: String, val dueAt: LocalDateTime)

/** Pure logic for the deadline reminders (unit-tested, no Spring / database needed). */
object ReminderComposer {

    /** Accepts "2026-06-20T23:59:00" or "2026-06-20" (date only = end of that day). */
    fun parseDue(raw: String): LocalDateTime? = try {
        LocalDateTime.parse(raw)
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(raw.take(10)).atTime(23, 59)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** Incomplete assignments due between [now] and [now] + [hours]. */
    fun dueSoon(
        assignments: List<AssignmentEntity>,
        now: LocalDateTime,
        hours: Long = 24
    ): List<DueItem> {
        val limit = now.plusHours(hours)
        return assignments.mapNotNull { a ->
            val owner = a.userId ?: return@mapNotNull null
            val due = parseDue(a.dueDate) ?: return@mapNotNull null
            if (!a.isCompleted && !due.isBefore(now) && !due.isAfter(limit)) {
                DueItem(owner, a.title, due)
            } else null
        }
    }

    fun byUser(items: List<DueItem>): Map<UUID, List<DueItem>> =
        items.groupBy { it.userId }.mapValues { (_, v) -> v.sortedBy { it.dueAt } }

    /** (title, body) of the notification for one user's due-soon items. */
    fun compose(items: List<DueItem>): Pair<String, String> {
        require(items.isNotEmpty()) { "No items to compose" }
        return if (items.size == 1) {
            "Assignment due soon" to "\"${items.first().title}\" is due within 24 hours."
        } else {
            val names = items.take(3).joinToString(", ") { it.title }
            val more = if (items.size > 3) " and ${items.size - 3} more" else ""
            "${items.size} assignments due soon" to "$names$more are due within 24 hours."
        }
    }
}

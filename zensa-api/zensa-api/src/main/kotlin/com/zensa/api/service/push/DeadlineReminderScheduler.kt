package com.zensa.api.service.push

import com.zensa.api.repository.AssignmentRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Real-time-ish deadline alerts: every morning (default 07:00 Africa/Johannesburg) each user with
 * incomplete assignments due in the next 24 hours receives ONE push notification.
 * Disabled automatically when Firebase is not configured.
 */
@Component
class DeadlineReminderScheduler(
    private val assignments: AssignmentRepository,
    private val notifications: NotificationService,
    @Value("\${zensa.reminders.enabled:true}") private val enabled: Boolean
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${zensa.reminders.cron:0 0 7 * * *}", zone = "\${zensa.reminders.zone:Africa/Johannesburg}")
    fun sendDeadlineReminders() {
        if (!enabled || !notifications.isConfigured) return

        val perUser = ReminderComposer.byUser(
            ReminderComposer.dueSoon(assignments.findAllIncomplete(), LocalDateTime.now())
        )
        var users = 0
        for ((userId, items) in perUser) {
            val (title, body) = ReminderComposer.compose(items)
            try {
                if (notifications.sendToUser(userId, title, body, "deadline_reminder") > 0) users++
            } catch (e: Exception) {
                log.warn("Reminder failed for one user: {}", e.javaClass.simpleName)
            }
        }
        log.info("Deadline reminders sent to {} users", users)
    }
}

package com.zensa.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zensa.app.R
import com.zensa.app.domain.model.Achievement
import com.zensa.app.domain.model.LevelInfo
import com.zensa.app.domain.usecase.SmartFocusUseCase

/**
 * UiText - turns stable domain keys and known backend/validation messages into
 * translated text.
 *
 * WHY this exists: the data and domain layers deliberately return plain English
 * constants (e.g. priority "HIGH", validation message "Email is required"). They
 * are used in unit tests and sent to the server, so they must not change with the
 * phone's language. The UI layer maps them to string resources here, so every
 * screen shows English, isiZulu, Sesotho or isiXhosa depending on the chosen language.
 *
 * Messages that are not recognised (e.g. a custom server message) are shown as-is.
 */
object UiText {

    /** Known English messages -> string resource. Looked up by exact text. */
    private val knownMessages: Map<String, Int> = mapOf(
        // Validation
        "Email is required" to R.string.err_email_required,
        "Enter a valid email address" to R.string.err_email_invalid,
        "Password is required" to R.string.err_password_required,
        "Password must be at least 8 characters" to R.string.err_password_min,
        "Name is required" to R.string.err_name_required,
        "Name cannot be empty" to R.string.err_name_empty,
        "Title is required" to R.string.err_title_required,
        "Write a title or some content" to R.string.err_note_empty,
        "Due date is required" to R.string.err_due_required,
        "Use format YYYY-MM-DD" to R.string.err_due_format,
        "Month must be between 1 and 12" to R.string.err_due_month,
        "Day must be between 1 and 31" to R.string.err_due_day,
        "Enter your current password" to R.string.err_current_password,
        "New password must be at least 8 characters" to R.string.err_new_password_min,
        "New passwords do not match" to R.string.err_passwords_mismatch,
        "New password must differ from current" to R.string.err_password_same,
        // Not found
        "File is too large (max 10 MB)" to R.string.err_file_too_large,
        "This file type or size is not allowed" to R.string.err_file_not_allowed,
        "File storage is not configured on the server" to R.string.err_storage_unavailable,
        "Could not read the selected file" to R.string.err_file_read,
        "Push notifications are not configured in this build" to R.string.err_push_not_configured_app,
        "Push notifications are not configured on the server" to R.string.err_push_not_configured_server,
        "Sign-in failed" to R.string.err_sso_failed,
        "Google sign-in is not configured yet" to R.string.err_sso_not_configured,
        "Module code is required" to R.string.err_module_code_required,
        "Module name is required" to R.string.err_module_name_required,
        "Module not found" to R.string.err_module_not_found,
        "Note not found" to R.string.err_note_not_found,
        "Assignment not found" to R.string.err_assignment_not_found,
        // Network / server
        "Cannot reach the server. Check your internet connection." to R.string.err_network,
        "Incorrect email or password" to R.string.err_bad_credentials,
        "An account with this email already exists" to R.string.err_email_exists,
        "Server error. Please try again later." to R.string.err_server,
        "Please check your details" to R.string.err_check_details,
        "Offline — will retry later" to R.string.err_offline_retry,
        // Sync
        "Sync complete" to R.string.sync_complete,
        "Sync failed" to R.string.sync_failed
    )

    /** Resource for a known message, or null when the message is unknown. */
    @StringRes
    fun messageResId(message: String): Int? {
        knownMessages[message]?.let { return it }
        return when {
            message.startsWith("Unexpected error") -> R.string.err_unexpected
            message.startsWith("Sync error") -> R.string.sync_failed
            message.startsWith("Sync failed") -> R.string.sync_failed
            message.startsWith("Offline — showing saved") -> R.string.err_offline_saved
            message.startsWith("Upload failed") -> R.string.err_upload_failed
            message.startsWith("Could not open the attachment") -> R.string.err_open_attachment
            message.startsWith("Could not send the test notification") -> R.string.err_push_failed
            else -> null
        }
    }

    private val sessionSavedRegex = Regex("""^Session saved \((\d+) min\)$""")

    /** Extracts N from "Session saved (N min)", or null for any other message. */
    fun sessionSavedMinutes(message: String): Int? =
        sessionSavedRegex.find(message)?.groupValues?.get(1)?.toIntOrNull()

    @StringRes
    fun priorityResId(priority: String): Int = when (priority.uppercase()) {
        "LOW" -> R.string.priority_low
        "HIGH" -> R.string.priority_high
        "URGENT" -> R.string.priority_urgent
        else -> R.string.priority_medium
    }

    @StringRes
    fun statusResId(status: String): Int = when (status.uppercase()) {
        "COMPLETED" -> R.string.status_completed
        "IN_PROGRESS" -> R.string.status_in_progress
        else -> R.string.status_pending
    }

    @StringRes
    fun focusReasonResId(reason: String): Int? = when (reason) {
        SmartFocusUseCase.REASON_OVERDUE -> R.string.reason_overdue
        SmartFocusUseCase.REASON_DUE_TODAY -> R.string.reason_due_today
        SmartFocusUseCase.REASON_DUE_SOON -> R.string.reason_due_soon
        SmartFocusUseCase.REASON_URGENT -> R.string.reason_urgent
        SmartFocusUseCase.REASON_HIGH_PRIORITY -> R.string.reason_high_priority
        SmartFocusUseCase.REASON_PARTLY_DONE -> R.string.reason_partly_done
        SmartFocusUseCase.REASON_IN_PROGRESS -> R.string.reason_in_progress
        else -> null
    }

    @StringRes
    fun badgeTitleResId(a: Achievement): Int = when (a) {
        Achievement.FIRST_STUDY_SESSION -> R.string.badge_first_study_session_title
        Achievement.STUDIED_60_MINUTES -> R.string.badge_studied_60_minutes_title
        Achievement.STUDIED_600_MINUTES -> R.string.badge_studied_600_minutes_title
        Achievement.STREAK_3_DAYS -> R.string.badge_streak_3_days_title
        Achievement.STREAK_7_DAYS -> R.string.badge_streak_7_days_title
        Achievement.STREAK_30_DAYS -> R.string.badge_streak_30_days_title
        Achievement.FIRST_ASSIGNMENT_COMPLETED -> R.string.badge_first_assignment_completed_title
        Achievement.FIVE_ASSIGNMENTS_COMPLETED -> R.string.badge_five_assignments_completed_title
        Achievement.TWENTY_ASSIGNMENTS_COMPLETED -> R.string.badge_twenty_assignments_completed_title
        Achievement.TEN_SUBTASKS_COMPLETED -> R.string.badge_ten_subtasks_completed_title
        Achievement.FIFTY_SUBTASKS_COMPLETED -> R.string.badge_fifty_subtasks_completed_title
    }

    @StringRes
    fun badgeDescriptionResId(a: Achievement): Int = when (a) {
        Achievement.FIRST_STUDY_SESSION -> R.string.badge_first_study_session_desc
        Achievement.STUDIED_60_MINUTES -> R.string.badge_studied_60_minutes_desc
        Achievement.STUDIED_600_MINUTES -> R.string.badge_studied_600_minutes_desc
        Achievement.STREAK_3_DAYS -> R.string.badge_streak_3_days_desc
        Achievement.STREAK_7_DAYS -> R.string.badge_streak_7_days_desc
        Achievement.STREAK_30_DAYS -> R.string.badge_streak_30_days_desc
        Achievement.FIRST_ASSIGNMENT_COMPLETED -> R.string.badge_first_assignment_completed_desc
        Achievement.FIVE_ASSIGNMENTS_COMPLETED -> R.string.badge_five_assignments_completed_desc
        Achievement.TWENTY_ASSIGNMENTS_COMPLETED -> R.string.badge_twenty_assignments_completed_desc
        Achievement.TEN_SUBTASKS_COMPLETED -> R.string.badge_ten_subtasks_completed_desc
        Achievement.FIFTY_SUBTASKS_COMPLETED -> R.string.badge_fifty_subtasks_completed_desc
    }

    @StringRes
    fun levelTitleResId(titleKey: String): Int = when (titleKey) {
        LevelInfo.TITLE_STEADY -> R.string.level_steady
        LevelInfo.TITLE_FOCUSED -> R.string.level_focused
        LevelInfo.TITLE_DEEP -> R.string.level_deep
        LevelInfo.TITLE_ZEN -> R.string.level_zen
        else -> R.string.level_starter
    }
}

// ---------------- Composable shortcuts ----------------

/** Translates a validation/server message when it is a known one; otherwise returns it unchanged. */
@Composable
fun localizedMessage(message: String): String {
    // "Session saved (25 min)" carries a number, so it needs a formatted string.
    UiText.sessionSavedMinutes(message)?.let { minutes ->
        return stringResource(R.string.session_saved, minutes)
    }
    return UiText.messageResId(message)?.let { stringResource(it) } ?: message
}

@Composable
fun priorityLabel(priority: String): String = stringResource(UiText.priorityResId(priority))

@Composable
fun statusLabel(status: String): String = stringResource(UiText.statusResId(status))

@Composable
fun focusReasonLabel(reason: String): String? =
    UiText.focusReasonResId(reason)?.let { stringResource(it) }

@Composable
fun levelTitle(titleKey: String): String = stringResource(UiText.levelTitleResId(titleKey))

/** "Overdue", "Due today", "Due tomorrow", "Due in 3 days" or "Due 2026-07-01" - translated. */
@Composable
fun dueLabel(daysUntilDue: Long, dueDateIso: String): String = when {
    daysUntilDue < 0L -> stringResource(R.string.due_overdue)
    daysUntilDue == 0L -> stringResource(R.string.due_today)
    daysUntilDue == 1L -> stringResource(R.string.due_tomorrow)
    daysUntilDue <= 7L -> stringResource(R.string.due_in_days, daysUntilDue.toInt())
    else -> stringResource(R.string.due_on, dueDateIso.substringBefore("T"))
}

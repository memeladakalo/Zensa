package com.zensa.app.notifications

/**
 * Pure (Android-free) notification logic so it can be unit-tested on the JVM.
 */

/** What to display for one incoming push message. */
data class PushContent(
    val title: String,
    val body: String,
    val type: String
)

object PushMessageParser {

    const val TYPE_GENERAL = "general"
    const val DEFAULT_TITLE = "Zensa"

    /**
     * Builds the content from either the FCM "notification" block (title/body) or, for
     * data-only messages, from the "title"/"body"/"type" keys of the data payload.
     * Returns null when there is nothing to show (empty message).
     */
    fun parse(
        notificationTitle: String?,
        notificationBody: String?,
        data: Map<String, String>
    ): PushContent? {
        val title = (notificationTitle ?: data["title"])?.trim().orEmpty()
        val body = (notificationBody ?: data["body"])?.trim().orEmpty()
        if (title.isEmpty() && body.isEmpty()) return null
        return PushContent(
            title = title.ifEmpty { DEFAULT_TITLE },
            body = body,
            type = data["type"]?.ifBlank { null } ?: TYPE_GENERAL
        )
    }
}

object NotificationPolicy {
    /**
     * A notification is shown only when the student has not switched reminders off in
     * Zensa's Settings AND Android allows notifications (permission + system setting).
     */
    fun shouldShow(userEnabled: Boolean, osAllows: Boolean): Boolean = userEnabled && osAllows

    /** Stable id per message type so a newer reminder replaces an older one of the same kind. */
    fun notificationId(type: String): Int = 1000 + (type.hashCode() and 0xFF)
}

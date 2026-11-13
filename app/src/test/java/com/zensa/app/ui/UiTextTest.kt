package com.zensa.app.ui

import com.zensa.app.R
import com.zensa.app.domain.model.Achievement
import com.zensa.app.domain.usecase.SmartFocusUseCase
import com.zensa.app.ui.common.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The mapping from English domain/validation text to translated string resources.
 * If a message is renamed in ValidationUtils but not here, these tests fail - so the
 * translations can never silently stop being used.
 */
class UiTextTest {

    @Test
    fun `known validation messages map to resources`() {
        assertEquals(R.string.err_email_required, UiText.messageResId("Email is required"))
        assertEquals(R.string.err_password_min, UiText.messageResId("Password must be at least 8 characters"))
        assertEquals(R.string.err_network, UiText.messageResId("Cannot reach the server. Check your internet connection."))
    }

    @Test
    fun `unexpected error prefix maps to the generic message`() {
        assertEquals(R.string.err_unexpected, UiText.messageResId("Unexpected error: java.lang.Boom"))
    }

    @Test
    fun `unknown messages are not translated`() {
        assertNull(UiText.messageResId("Some custom server message"))
    }

    @Test
    fun `session saved message carries its minutes`() {
        assertEquals(25, UiText.sessionSavedMinutes("Session saved (25 min)"))
        assertNull(UiText.sessionSavedMinutes("Session saved"))
    }

    @Test
    fun `priority and status map and default safely`() {
        assertEquals(R.string.priority_urgent, UiText.priorityResId("urgent"))
        assertEquals(R.string.priority_medium, UiText.priorityResId("anything"))
        assertEquals(R.string.status_completed, UiText.statusResId("COMPLETED"))
        assertEquals(R.string.status_pending, UiText.statusResId("whatever"))
    }

    @Test
    fun `every smart focus reason has a translation`() {
        listOf(
            SmartFocusUseCase.REASON_OVERDUE, SmartFocusUseCase.REASON_DUE_TODAY, SmartFocusUseCase.REASON_DUE_SOON,
            SmartFocusUseCase.REASON_URGENT, SmartFocusUseCase.REASON_HIGH_PRIORITY,
            SmartFocusUseCase.REASON_PARTLY_DONE, SmartFocusUseCase.REASON_IN_PROGRESS
        ).forEach { assertNotNull("missing translation for $it", UiText.focusReasonResId(it)) }
    }

    @Test
    fun `every badge has its own title and description`() {
        val titles = Achievement.entries.map { UiText.badgeTitleResId(it) }
        val descriptions = Achievement.entries.map { UiText.badgeDescriptionResId(it) }
        assertEquals(Achievement.entries.size, titles.toSet().size)
        assertEquals(Achievement.entries.size, descriptions.toSet().size)
        assertNotEquals(titles, descriptions)
    }
}

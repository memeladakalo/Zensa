package com.zensa.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Notification logic that does not need a device (parsing + the show/hide decision). */
class PushMessageLogicTest {

    @Test
    fun `notification block is used when present`() {
        val c = PushMessageParser.parse("Deadline", "DB Design is due", mapOf("type" to "deadline_reminder"))
        assertNotNull(c)
        assertEquals("Deadline", c!!.title)
        assertEquals("DB Design is due", c.body)
        assertEquals("deadline_reminder", c.type)
    }

    @Test
    fun `data only message falls back to data keys`() {
        val c = PushMessageParser.parse(null, null, mapOf("title" to "Hi", "body" to "There"))
        assertEquals("Hi", c!!.title)
        assertEquals("There", c.body)
        assertEquals(PushMessageParser.TYPE_GENERAL, c.type)
    }

    @Test
    fun `empty message yields nothing to show`() {
        assertNull(PushMessageParser.parse(null, null, emptyMap()))
        assertNull(PushMessageParser.parse("  ", " ", emptyMap()))
    }

    @Test
    fun `missing title defaults to the app name`() {
        assertEquals(PushMessageParser.DEFAULT_TITLE, PushMessageParser.parse(null, "Body only", emptyMap())!!.title)
    }

    @Test
    fun `shown only when the user and the OS both allow it`() {
        assertTrue(NotificationPolicy.shouldShow(userEnabled = true, osAllows = true))
        assertFalse(NotificationPolicy.shouldShow(userEnabled = false, osAllows = true))
        assertFalse(NotificationPolicy.shouldShow(userEnabled = true, osAllows = false))
    }

    @Test
    fun `same type reuses the same notification id and different types differ`() {
        assertEquals(NotificationPolicy.notificationId("deadline_reminder"), NotificationPolicy.notificationId("deadline_reminder"))
        assertNotEquals(NotificationPolicy.notificationId("deadline_reminder"), NotificationPolicy.notificationId("achievement"))
    }
}

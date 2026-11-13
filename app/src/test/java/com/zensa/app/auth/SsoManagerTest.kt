package com.zensa.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SSO handling that can be tested locally (the browser flow itself needs a device + Google client id). */
class SsoManagerTest {

    @Test
    fun `real google client id counts as configured`() {
        assertTrue(SsoManager.isClientIdConfigured("1234567890-abcdef.apps.googleusercontent.com"))
    }

    @Test
    fun `blank or malformed client id counts as not configured`() {
        assertFalse(SsoManager.isClientIdConfigured(""))
        assertFalse(SsoManager.isClientIdConfigured("not-a-client-id"))
        assertFalse(SsoManager.isClientIdConfigured(".apps.googleusercontent.com"))
    }

    @Test
    fun `endpoints are google's official oauth endpoints`() {
        assertEquals("https://accounts.google.com/o/oauth2/v2/auth", SsoManager.AUTH_ENDPOINT)
        assertEquals("https://oauth2.googleapis.com/token", SsoManager.TOKEN_ENDPOINT)
    }

    @Test
    fun `id token never appears in toString`() {
        val outcome = SsoOutcome.IdToken("super-secret-token-value")
        assertFalse(outcome.toString().contains("super-secret"))
    }

    @Test
    fun `cancel and failure outcomes are distinct`() {
        assertTrue(SsoOutcome.Cancelled !is SsoOutcome.Failed)
        assertEquals("Sign-in failed", (SsoOutcome.Failed("Sign-in failed")).reason)
    }
}

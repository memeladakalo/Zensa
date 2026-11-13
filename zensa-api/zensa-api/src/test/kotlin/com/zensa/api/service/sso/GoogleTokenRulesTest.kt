package com.zensa.api.service.sso

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The claim checks applied to a Google ID token after its signature has been verified. */
class GoogleTokenRulesTest {

    @Test
    fun `both google issuer spellings are accepted`() {
        assertTrue(GoogleTokenRules.isValidIssuer("https://accounts.google.com"))
        assertTrue(GoogleTokenRules.isValidIssuer("accounts.google.com"))
    }

    @Test
    fun `other issuers are rejected`() {
        assertFalse(GoogleTokenRules.isValidIssuer("https://evil.example.com"))
        assertFalse(GoogleTokenRules.isValidIssuer(null))
    }

    @Test
    fun `audience must be one of our client ids`() {
        val allowed = GoogleTokenRules.parseClientIds(" id-1.apps.googleusercontent.com , id-2 ")
        assertTrue(GoogleTokenRules.audienceAllowed(listOf("id-2"), allowed))
        assertFalse(GoogleTokenRules.audienceAllowed(listOf("someone-elses-app"), allowed))
        assertFalse(GoogleTokenRules.audienceAllowed(null, allowed))
    }

    @Test
    fun `empty configuration never allows any audience`() {
        assertFalse(GoogleTokenRules.audienceAllowed(listOf("x"), emptySet()))
        assertTrue(GoogleTokenRules.parseClientIds("").isEmpty())
    }

    @Test
    fun `email_verified accepts boolean or string`() {
        assertTrue(GoogleTokenRules.asBoolean(true))
        assertTrue(GoogleTokenRules.asBoolean("TRUE"))
        assertFalse(GoogleTokenRules.asBoolean("false"))
        assertFalse(GoogleTokenRules.asBoolean(null))
        assertEquals(false, GoogleTokenRules.asBoolean(1))
    }
}

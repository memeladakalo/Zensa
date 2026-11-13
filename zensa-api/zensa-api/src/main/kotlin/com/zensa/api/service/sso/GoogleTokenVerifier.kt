package com.zensa.api.service.sso

import com.zensa.api.exception.ServiceUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Service

/** What Zensa needs to know about a signed-in Google user. */
data class GoogleIdentity(
    val subject: String,
    val email: String,
    val emailVerified: Boolean,
    val name: String?
)

/** Verifies a Google ID token (the result of the OAuth 2.0 + PKCE flow on the phone). */
interface GoogleTokenVerifier {
    /** @throws IllegalArgumentException (message starts with "Incorrect") when the token is invalid. */
    fun verify(idToken: String): GoogleIdentity
}

/** Pure rules, unit-tested without any network access. */
object GoogleTokenRules {
    private val ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")

    fun isValidIssuer(issuer: String?): Boolean = issuer != null && issuer in ISSUERS

    /** The token must have been issued for one of OUR OAuth client ids (stops tokens meant for other apps). */
    fun audienceAllowed(audiences: List<String>?, allowed: Set<String>): Boolean =
        allowed.isNotEmpty() && audiences.orEmpty().any { it in allowed }

    fun parseClientIds(raw: String): Set<String> =
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun asBoolean(value: Any?): Boolean = when (value) {
        is Boolean -> value
        is String -> value.equals("true", ignoreCase = true)
        else -> false
    }
}

/**
 * Real verification using Google's public signing keys (JWKS):
 *   - signature (RS256) is checked against https://www.googleapis.com/oauth2/v3/certs
 *   - expiry / not-before are checked by Spring's default validators
 *   - issuer must be Google; audience must be one of our configured client ids
 * No Google client secret is needed or stored by the server.
 */
@Service
class NimbusGoogleTokenVerifier(
    @Value("\${zensa.google.client-ids:}") clientIds: String
) : GoogleTokenVerifier {

    private val log = LoggerFactory.getLogger(javaClass)
    private val allowedAudiences = GoogleTokenRules.parseClientIds(clientIds)

    private val decoder: JwtDecoder by lazy {
        NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build()
    }

    override fun verify(idToken: String): GoogleIdentity {
        if (allowedAudiences.isEmpty()) {
            throw ServiceUnavailableException("Google sign-in is not configured on the server")
        }

        val jwt = try {
            decoder.decode(idToken)
        } catch (e: JwtException) {
            // Never log the token itself.
            log.warn("Google ID token rejected: {}", e.javaClass.simpleName)
            throw IllegalArgumentException("Incorrect Google sign-in token")
        }

        if (!GoogleTokenRules.isValidIssuer(jwt.getClaimAsString("iss")) ||
            !GoogleTokenRules.audienceAllowed(jwt.audience, allowedAudiences)
        ) {
            log.warn("Google ID token rejected: wrong issuer or audience")
            throw IllegalArgumentException("Incorrect Google sign-in token")
        }

        val email = jwt.getClaimAsString("email")
            ?: throw IllegalArgumentException("Incorrect Google sign-in token: no email")

        return GoogleIdentity(
            subject = jwt.subject ?: "",
            email = email,
            emailVerified = GoogleTokenRules.asBoolean(jwt.getClaim<Any>("email_verified")),
            name = jwt.getClaimAsString("name")
        )
    }

    private companion object {
        const val JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs"
    }
}

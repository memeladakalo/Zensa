package com.zensa.api.service

import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

/**
 * CurrentUser - the id of the authenticated caller.
 * JwtAuthFilter stores the userId claim of the verified JWT in Authentication.details.
 * Every Part 3 endpoint derives the owner ONLY from here, never from request parameters,
 * so a user can never read or write another user's data.
 */
object CurrentUser {
    fun id(): UUID? {
        val raw = SecurityContextHolder.getContext().authentication?.details as? String ?: return null
        return try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

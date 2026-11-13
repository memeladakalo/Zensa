package com.zensa.api.service

import com.zensa.api.config.JwtService
import com.zensa.api.entity.UserEntity
import com.zensa.api.repository.UserRepository
import com.zensa.api.service.sso.GoogleIdentity
import com.zensa.api.service.sso.GoogleTokenVerifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/** SSO login flow in AuthService, using a real (in-memory H2) UserRepository and a fake Google verifier. */
@DataJpaTest
class AuthServiceGoogleTest @Autowired constructor(private val users: UserRepository) {

    private val encoder = BCryptPasswordEncoder()
    private val jwt = JwtService("test-only-secret-key-at-least-32-characters-long", 3_600_000)

    private fun service(identity: GoogleIdentity?) = AuthService(
        users, encoder, jwt,
        object : GoogleTokenVerifier {
            override fun verify(idToken: String): GoogleIdentity =
                identity ?: throw IllegalArgumentException("Incorrect Google sign-in token")
        }
    )

    @Test
    fun `first google sign-in creates a user and returns a valid zensa jwt`() {
        val response = service(GoogleIdentity("sub-1", "Student@Example.com", true, "Thembela"))
            .loginWithGoogle("any-token")

        assertEquals("student@example.com", response.email)
        assertEquals("Thembela", response.name)
        assertTrue(jwt.isValid(response.token))
        assertNotNull(users.findByEmail("student@example.com"))
    }

    @Test
    fun `second google sign-in reuses the same account`() {
        val svc = service(GoogleIdentity("sub-1", "again@example.com", true, "Again"))
        val first = svc.loginWithGoogle("t")
        val second = svc.loginWithGoogle("t")
        assertEquals(first.userId, second.userId)
        assertEquals(1, users.findAll().count { it.email == "again@example.com" })
    }

    @Test
    fun `existing password account is linked by email`() {
        val existing = users.save(
            UserEntity(name = "Existing", email = "linked@example.com", passwordHash = encoder.encode("password123"))
        )
        val response = service(GoogleIdentity("sub-2", "linked@example.com", true, "Other Name"))
            .loginWithGoogle("t")
        assertEquals(existing.userId.toString(), response.userId)
    }

    @Test
    fun `unverified google email is refused`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            service(GoogleIdentity("sub-3", "nope@example.com", false, null)).loginWithGoogle("t")
        }
        assertTrue(ex.message!!.startsWith("Incorrect"))
    }

    @Test
    fun `invalid token is refused with an Incorrect message`() {
        val ex = assertThrows(IllegalArgumentException::class.java) { service(null).loginWithGoogle("bad") }
        assertTrue(ex.message!!.startsWith("Incorrect"))
    }

    @Test
    fun `sso account cannot be entered with a guessed password`() {
        service(GoogleIdentity("sub-4", "sso@example.com", true, null)).loginWithGoogle("t")
        val stored = users.findByEmail("sso@example.com")!!
        assertTrue(!encoder.matches("password123", stored.passwordHash))
        assertEquals("sso", stored.name) // falls back to the part before the @
    }
}

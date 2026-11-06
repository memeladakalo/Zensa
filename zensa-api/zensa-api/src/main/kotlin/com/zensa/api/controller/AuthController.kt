package com.zensa.api.controller

import com.zensa.api.dto.LoginRequest
import com.zensa.api.dto.LoginResponse
import com.zensa.api.dto.RegisterRequest
import com.zensa.api.dto.RegisterResponse
import com.zensa.api.dto.SsoRequest
import com.zensa.api.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<RegisterResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<LoginResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    /**
     * Part 3 SSO: client sends Google (or other) idToken; server verifies and returns app JWT.
     * Wire Google token verification using [zensa.sso.google.client-id] when credentials are set.
     */
    @PostMapping("/sso")
    fun sso(@RequestBody request: SsoRequest): ResponseEntity<LoginResponse> {
        val response = authService.loginWithSso(request)
        return ResponseEntity.ok(response)
    }
}

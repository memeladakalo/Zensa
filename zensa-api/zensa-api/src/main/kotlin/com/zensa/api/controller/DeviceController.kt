package com.zensa.api.controller

import com.zensa.api.dto.DeviceTokenRequest
import com.zensa.api.entity.DeviceTokenEntity
import com.zensa.api.repository.DeviceTokenRepository
import com.zensa.api.service.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/** Registers / unregisters the caller's phone for push notifications (FCM token). */
@RestController
@RequestMapping("/api/devices")
class DeviceController(private val devices: DeviceTokenRepository) {

    @PostMapping
    fun register(@Valid @RequestBody request: DeviceTokenRequest): ResponseEntity<Void> {
        val userId = CurrentUser.id() ?: return ResponseEntity.status(401).build()
        val token = request.token.trim()

        val existing = devices.findByToken(token)
        if (existing != null) {
            // Same phone, possibly a different account now: re-assign it to the caller.
            existing.userId = userId
            existing.platform = request.platform
            existing.updatedAt = LocalDateTime.now()
            devices.save(existing)
        } else {
            devices.save(DeviceTokenEntity(userId = userId, token = token, platform = request.platform))
        }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping
    fun unregister(@Valid @RequestBody request: DeviceTokenRequest): ResponseEntity<Void> {
        val userId = CurrentUser.id() ?: return ResponseEntity.status(401).build()
        val existing = devices.findByToken(request.token.trim())
        // Only the owner may remove a registration; unknown tokens are a harmless no-op.
        if (existing != null && existing.userId == userId) devices.delete(existing)
        return ResponseEntity.noContent().build()
    }
}

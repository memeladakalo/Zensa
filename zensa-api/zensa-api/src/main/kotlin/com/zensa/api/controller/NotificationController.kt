package com.zensa.api.controller

import com.zensa.api.dto.TestNotificationResponse
import com.zensa.api.service.CurrentUser
import com.zensa.api.service.push.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val notifications: NotificationService) {

    /** Sends a REAL push notification (through Firebase Cloud Messaging) to the caller's phones. */
    @PostMapping("/test")
    fun test(): ResponseEntity<TestNotificationResponse> {
        val userId = CurrentUser.id() ?: return ResponseEntity.status(401).build()
        val sent = notifications.sendToUser(
            userId = userId,
            title = "Zensa",
            body = "Push notifications are working. Calm, focused studying!",
            type = "test"
        )
        val message = if (sent > 0) "Sent to $sent device(s)" else "No registered device found for this account"
        return ResponseEntity.ok(TestNotificationResponse(sent, message))
    }
}

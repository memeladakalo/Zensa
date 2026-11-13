package com.zensa.api.service.push

import com.zensa.api.exception.ServiceUnavailableException
import com.zensa.api.repository.DeviceTokenRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * NotificationService - sends a push notification to every phone registered by a user.
 * Tokens that Firebase reports as unregistered are deleted so they are not retried forever.
 */
@Service
class NotificationService(
    private val deviceTokens: DeviceTokenRepository,
    private val sender: PushSender
) {
    val isConfigured: Boolean get() = sender.isConfigured

    /** @return the number of devices the message was accepted for. */
    fun sendToUser(userId: UUID, title: String, body: String, type: String): Int {
        if (!sender.isConfigured) {
            throw ServiceUnavailableException("Push notifications are not configured on the server")
        }
        var sent = 0
        for (device in deviceTokens.findAllByUserId(userId)) {
            when (sender.send(device.token, title, body, mapOf("type" to type))) {
                PushResult.SENT -> sent++
                PushResult.INVALID_TOKEN -> deviceTokens.delete(device)
                PushResult.FAILED -> Unit
            }
        }
        return sent
    }
}

package com.zensa.api.service.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory

enum class PushResult { SENT, INVALID_TOKEN, FAILED }

/** Sends one push message to one device token. */
interface PushSender {
    /** False when Firebase credentials have not been provided (push is then simply unavailable). */
    val isConfigured: Boolean

    fun send(token: String, title: String, body: String, data: Map<String, String> = emptyMap()): PushResult
}

/** Real sender using the Firebase Admin SDK (Firebase Cloud Messaging HTTP v1). */
class FirebasePushSender(private val messaging: FirebaseMessaging) : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)
    override val isConfigured: Boolean = true

    override fun send(token: String, title: String, body: String, data: Map<String, String>): PushResult {
        val message = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .build()
        return try {
            messaging.send(message)
            PushResult.SENT
        } catch (e: FirebaseMessagingException) {
            if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                PushResult.INVALID_TOKEN // phone uninstalled the app / token expired
            } else {
                log.warn("FCM send failed: {}", e.messagingErrorCode)
                PushResult.FAILED
            }
        }
    }
}

/** Used when Firebase is not configured. */
class UnconfiguredPushSender : PushSender {
    override val isConfigured: Boolean = false
    override fun send(token: String, title: String, body: String, data: Map<String, String>) =
        PushResult.FAILED
}

package com.zensa.app.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ZensaMessagingService - receives Firebase Cloud Messaging events.
 *
 *  - onNewToken: FCM issued a new registration token -> send it to the Zensa API.
 *  - onMessageReceived: runs when a message arrives while the app is in the FOREGROUND (and
 *    for data-only messages in any state). We display it ourselves with the Zensa channel.
 *    Messages with a "notification" block that arrive while the app is in the BACKGROUND
 *    are displayed by the system itself using the default channel in AndroidManifest.xml.
 */
class ZensaMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM token issued")
        scope.launch { ServiceLocator.pushTokenRegistrar.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val content = PushMessageParser.parse(
            notificationTitle = message.notification?.title,
            notificationBody = message.notification?.body,
            data = message.data
        ) ?: return

        // Respect the student's choice in Settings (Notifications switch).
        val userEnabled = runBlocking {
            ServiceLocator.userPreferences().notificationsEnabled.first()
        }
        val allowed = NotificationPolicy.shouldShow(
            userEnabled = userEnabled,
            osAllows = NotificationHelper.osAllowsNotifications(this)
        )
        Log.i(TAG, "Push received (type=${content.type}), showing=$allowed")

        if (allowed) {
            NotificationHelper.show(
                context = this,
                title = content.title,
                body = content.body,
                notificationId = NotificationPolicy.notificationId(content.type)
            )
        }
    }

    private companion object {
        const val TAG = "ZensaMessagingService"
    }
}

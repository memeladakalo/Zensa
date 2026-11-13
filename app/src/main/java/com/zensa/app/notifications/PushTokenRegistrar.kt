package com.zensa.app.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.DeviceTokenRequestDto
import com.zensa.app.data.remote.dto.TestNotificationResultDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

/**
 * PushTokenRegistrar - tells the Zensa API which phone should receive this user's push
 * notifications (the Firebase Cloud Messaging registration token).
 *
 * Firebase is optional at build time: when app/google-services.json is missing,
 * FirebaseApp is not initialised and every method here quietly does nothing, so the
 * app still runs. See INSTALLATION_AND_CONFIGURATION_GUIDE.md to enable push.
 */
class PushTokenRegistrar(
    private val context: Context,
    private val api: ZensaApiService,
    private val tokenStorage: TokenStorage
) {

    private fun firebaseReady(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    /** Fetches this phone's current FCM token (null when Firebase is not configured). */
    private suspend fun currentToken(): String? {
        if (!firebaseReady()) return null
        return suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                val value = if (task.isSuccessful) task.result else null
                if (cont.isActive) cont.resume(value)
            }
        }
    }

    /** Registers the current token for the signed-in user. Best effort; never throws. */
    suspend fun registerCurrentToken() = withContext(Dispatchers.IO) {
        if (!tokenStorage.hasToken()) return@withContext
        val token = currentToken() ?: return@withContext
        register(token)
    }

    /** Registers a specific token (used by onNewToken). */
    suspend fun register(token: String) = withContext(Dispatchers.IO) {
        if (!tokenStorage.hasToken()) return@withContext // not signed in yet; registered after login
        try {
            val response = api.registerDevice(DeviceTokenRequestDto(token))
            Log.i(TAG, "Device registration: HTTP ${response.code()}") // never log the token itself
        } catch (e: Exception) {
            Log.w(TAG, "Device registration failed: ${e.javaClass.simpleName}")
        }
    }

    /** Called on logout so the next person on this phone does not receive this user's reminders. */
    suspend fun unregisterCurrentToken() = withContext(Dispatchers.IO) {
        val token = currentToken() ?: return@withContext
        try {
            api.unregisterDevice(DeviceTokenRequestDto(token))
        } catch (e: Exception) {
            Log.w(TAG, "Device unregistration failed: ${e.javaClass.simpleName}")
        }
    }

    /** Asks the backend to send a real push to this user's registered phones (Settings > test). */
    suspend fun sendTestNotification(): ApiResult<TestNotificationResultDto> =
        withContext(Dispatchers.IO) {
            try {
                if (!firebaseReady()) {
                    return@withContext ApiResult.Error("Push notifications are not configured in this build")
                }
                registerCurrentToken() // make sure this phone is registered first
                val response = api.sendTestNotification()
                val body = response.body()
                when {
                    response.isSuccessful && body != null -> ApiResult.Success(body)
                    response.code() == 503 -> ApiResult.Error("Push notifications are not configured on the server")
                    else -> ApiResult.Error("Could not send the test notification (${response.code()})")
                }
            } catch (e: IOException) {
                ApiResult.Error("Cannot reach the server. Check your internet connection.")
            } catch (e: Exception) {
                ApiResult.Error("Unexpected error: ${e.message}")
            }
        }

    private companion object {
        const val TAG = "PushTokenRegistrar"
    }
}

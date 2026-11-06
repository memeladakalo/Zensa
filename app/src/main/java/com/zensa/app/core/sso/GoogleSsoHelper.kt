package com.zensa.app.core.sso

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

/**
 * Google SSO helper for Part 3.
 * Replace [WEB_CLIENT_ID] with your OAuth 2.0 Web client ID from Google Cloud Console.
 * The idToken is sent to the Spring Boot /auth/sso endpoint for JWT exchange.
 */
object GoogleSsoHelper {

    // TODO: Replace with real Web client ID from Google Cloud Console
    const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun signInIntent(context: Context): Intent = getClient(context).signInIntent

    fun accountFromIntent(data: Intent?): GoogleSignInAccount? {
        if (data == null) return null
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            null
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        getClient(context).signOut().addOnCompleteListener { onComplete() }
    }
}

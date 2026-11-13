package com.zensa.app.ui.screens.login

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.auth.SsoManager
import com.zensa.app.auth.SsoOutcome
import com.zensa.app.core.network.ApiResult
import com.zensa.app.core.util.ValidationUtils
import com.zensa.app.data.repository.AuthRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * LoginViewModel
 *
 * Owns the state of the Login screen. The Compose UI never changes
 * state directly - it only calls methods here and reads [uiState].
 *
 * Flow when the user taps Login:
 *   1. Validate fields locally (ValidationUtils).
 *   2. If invalid -> set field errors and stop; the repository is never called.
 *   3. Otherwise show loading and call AuthRepository.login(...), which sends
 *      a real POST /api/auth/login request and stores the returned JWT.
 *   4. On success -> invoke onSuccess (the UI navigates away).
 *   5. On failure -> show a friendly generalError.
 *
 * The authRepository parameter has a default so the ViewModel is built
 * normally in production and can receive a fake in unit tests.
 */
class LoginViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val ssoManager: SsoManager = ServiceLocator.ssoManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(v: String) = _uiState.update {
        it.copy(email = v, emailError = null, generalError = null)
    }

    fun onPasswordChange(v: String) = _uiState.update {
        it.copy(password = v, passwordError = null, generalError = null)
    }

    fun togglePasswordVisibility() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    // ---------------- Single Sign-On (Google, OAuth 2.0 + PKCE) ----------------

    /** True when a Google OAuth client id has been configured for this build. */
    val ssoConfigured: Boolean get() = ssoManager.isConfigured

    /** The intent that opens the Google sign-in page; launch it with an ActivityResult launcher. */
    fun createSsoIntent(): Intent = ssoManager.createSignInIntent()

    /** The user tapped "Continue with Google" but SSO has not been configured in this build. */
    fun onSsoNotConfigured() {
        _uiState.update { it.copy(generalError = "Google sign-in is not configured yet") }
    }

    /**
     * Called with the result of the browser sign-in:
     *   cancelled  -> back to the form, no error shown
     *   failure    -> friendly error
     *   ID token   -> backend verifies it and returns a Zensa JWT, then onSuccess()
     */
    fun onSsoResult(data: Intent?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSsoLoading = true, generalError = null) }

            when (val outcome = ssoManager.completeSignIn(data)) {
                is SsoOutcome.IdToken -> {
                    when (val result = authRepository.loginWithGoogle(outcome.value)) {
                        is ApiResult.Success -> {
                            Log.i(TAG, "SSO sign-in succeeded")
                            _uiState.update { it.copy(isSsoLoading = false) }
                            onSuccess()
                        }
                        is ApiResult.Error -> _uiState.update {
                            it.copy(isSsoLoading = false, generalError = result.message)
                        }
                        ApiResult.Loading -> Unit
                    }
                }
                SsoOutcome.Cancelled -> {
                    Log.i(TAG, "SSO cancelled by the user")
                    _uiState.update { it.copy(isSsoLoading = false) }
                }
                is SsoOutcome.Failed -> _uiState.update {
                    it.copy(isSsoLoading = false, generalError = outcome.reason)
                }
                SsoOutcome.NotConfigured -> _uiState.update {
                    it.copy(isSsoLoading = false, generalError = "Google sign-in is not configured yet")
                }
            }
        }
    }

    fun login(onSuccess: () -> Unit) {
        val s = _uiState.value
        val emailError = ValidationUtils.emailError(s.email)
        val passwordError = ValidationUtils.passwordError(s.password)

        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            when (val result = authRepository.login(s.email.trim(), s.password)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, generalError = result.message) }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private companion object {
        const val TAG = "LoginViewModel"
    }
}

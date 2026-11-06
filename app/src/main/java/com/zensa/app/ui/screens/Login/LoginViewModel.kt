package com.zensa.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val authRepository: AuthRepository = ServiceLocator.authRepository
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
}

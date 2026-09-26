package com.taskplanner.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskplanner.app.domain.model.AuthOperationException
import com.taskplanner.app.domain.model.AuthValidationException
import com.taskplanner.app.domain.repository.AuthRepository
import com.taskplanner.app.domain.usecase.AuthInputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTER, RESET_PASSWORD }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val loading: Boolean = false,
    val configured: Boolean = true,
    val signedIn: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState(configured = auth.isConfigured))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setMode(mode: AuthMode) {
        _state.value = _state.value.copy(
            mode = mode, password = "", confirmation = "", message = null, error = null
        )
    }
    fun setEmail(value: String) { _state.value = _state.value.copy(email = value, error = null) }
    fun setPassword(value: String) { _state.value = _state.value.copy(password = value, error = null) }
    fun setConfirmation(value: String) { _state.value = _state.value.copy(confirmation = value, error = null) }

    fun submit() {
        if (_state.value.loading || !auth.isConfigured) return
        val current = _state.value
        viewModelScope.launch {
            _state.value = current.copy(loading = true, error = null, message = null)
            try {
                when (current.mode) {
                    AuthMode.LOGIN -> {
                        val input = AuthInputValidator.login(current.email, current.password)
                        auth.signIn(input.email, input.password)
                        _state.value = _state.value.copy(loading = false, signedIn = true)
                    }
                    AuthMode.REGISTER -> {
                        val input = AuthInputValidator.register(current.email, current.password, current.confirmation)
                        auth.register(input.email, input.password)
                        _state.value = _state.value.copy(loading = false, signedIn = true)
                    }
                    AuthMode.RESET_PASSWORD -> {
                        auth.sendPasswordReset(AuthInputValidator.email(current.email))
                        _state.value = _state.value.copy(
                            loading = false, mode = AuthMode.LOGIN,
                            message = "Письмо для сброса пароля отправлено"
                        )
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.value = _state.value.copy(
                    loading = false,
                    error = when (error) {
                        is AuthValidationException, is AuthOperationException -> error.message
                        else -> "Не удалось выполнить действие. Повторите попытку"
                    }
                )
            }
        }
    }
}

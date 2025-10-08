package com.meneses.budgethunter.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.error_confirm_password_required
import budgethunter.composeapp.generated.resources.error_email_required
import budgethunter.composeapp.generated.resources.error_name_required
import budgethunter.composeapp.generated.resources.error_password_required
import budgethunter.composeapp.generated.resources.error_password_too_short
import budgethunter.composeapp.generated.resources.error_passwords_do_not_match
import budgethunter.composeapp.generated.resources.error_sign_up_failed
import com.meneses.budgethunter.auth.application.SignUpEvent
import com.meneses.budgethunter.auth.application.SignUpState
import com.meneses.budgethunter.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(SignUpState())

    fun sendEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.EmailChanged -> updateEmail(event.email)
            is SignUpEvent.NameChanged -> updateName(event.name)
            is SignUpEvent.PasswordChanged -> updatePassword(event.password)
            is SignUpEvent.ConfirmPasswordChanged -> updateConfirmPassword(event.confirmPassword)
            is SignUpEvent.SignUpClicked -> signUp()
            is SignUpEvent.DismissError -> dismissError()
        }
    }

    private fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    private fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword) }
    }

    private fun signUp() {
        val currentState = _uiState.value

        // Validation
        when {
            currentState.email.isBlank() -> {
                _uiState.update { it.copy(error = Res.string.error_email_required) }
                return
            }
            currentState.name.isBlank() -> {
                _uiState.update { it.copy(error = Res.string.error_name_required) }
                return
            }
            currentState.password.isBlank() -> {
                _uiState.update { it.copy(error = Res.string.error_password_required) }
                return
            }
            currentState.confirmPassword.isBlank() -> {
                _uiState.update { it.copy(error = Res.string.error_confirm_password_required) }
                return
            }
            currentState.password != currentState.confirmPassword -> {
                _uiState.update { it.copy(error = Res.string.error_passwords_do_not_match) }
                return
            }
            currentState.password.length < 6 -> {
                _uiState.update { it.copy(error = Res.string.error_password_too_short) }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signUp(
                email = currentState.email,
                name = currentState.name,
                password = currentState.password
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedUp = true
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = Res.string.error_sign_up_failed
                        )
                    }
                }
            )
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

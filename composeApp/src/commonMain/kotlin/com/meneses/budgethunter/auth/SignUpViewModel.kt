package com.meneses.budgethunter.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            is SignUpEvent.NavigateToSignIn -> {
                // Navigation handled by UI
            }
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
                _uiState.update { it.copy(error = "Email is required") }
                return
            }
            currentState.name.isBlank() -> {
                _uiState.update { it.copy(error = "Name is required") }
                return
            }
            currentState.password.isBlank() -> {
                _uiState.update { it.copy(error = "Password is required") }
                return
            }
            currentState.confirmPassword.isBlank() -> {
                _uiState.update { it.copy(error = "Please confirm your password") }
                return
            }
            currentState.password != currentState.confirmPassword -> {
                _uiState.update { it.copy(error = "Passwords do not match") }
                return
            }
            currentState.password.length < 6 -> {
                _uiState.update { it.copy(error = "Password must be at least 6 characters") }
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
                            error = exception.message ?: "Sign up failed"
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

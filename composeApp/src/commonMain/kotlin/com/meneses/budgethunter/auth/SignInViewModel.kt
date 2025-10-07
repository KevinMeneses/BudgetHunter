package com.meneses.budgethunter.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.auth.application.SignInEvent
import com.meneses.budgethunter.auth.application.SignInState
import com.meneses.budgethunter.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(SignInState())

    fun sendEvent(event: SignInEvent) {
        when (event) {
            is SignInEvent.EmailChanged -> updateEmail(event.email)
            is SignInEvent.PasswordChanged -> updatePassword(event.password)
            is SignInEvent.SignInClicked -> signIn()
            is SignInEvent.NavigateToSignUp -> {
                // Navigation handled by UI
            }
            is SignInEvent.DismissError -> dismissError()
        }
    }

    private fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    private fun signIn() {
        val currentState = _uiState.value

        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signIn(
                email = currentState.email,
                password = currentState.password
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = true
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Sign in failed"
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

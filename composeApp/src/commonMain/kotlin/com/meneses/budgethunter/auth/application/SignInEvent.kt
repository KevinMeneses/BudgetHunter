package com.meneses.budgethunter.auth.application

sealed interface SignInEvent {
    data class EmailChanged(val email: String) : SignInEvent
    data class PasswordChanged(val password: String) : SignInEvent
    data object SignInClicked : SignInEvent
    data object NavigateToSignUp : SignInEvent
    data object DismissError : SignInEvent
}

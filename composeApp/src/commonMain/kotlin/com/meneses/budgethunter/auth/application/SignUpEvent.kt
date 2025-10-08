package com.meneses.budgethunter.auth.application

sealed interface SignUpEvent {
    data class EmailChanged(val email: String) : SignUpEvent
    data class NameChanged(val name: String) : SignUpEvent
    data class PasswordChanged(val password: String) : SignUpEvent
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpEvent
    data object SignUpClicked : SignUpEvent
    data object DismissError : SignUpEvent
}

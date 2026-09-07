package com.meneses.budgethunter.auth.application

sealed interface SignUpIntent {
    data class EmailChanged(val email: String) : SignUpIntent
    data class NameChanged(val name: String) : SignUpIntent
    data class PasswordChanged(val password: String) : SignUpIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpIntent
    data object SignUpClicked : SignUpIntent
    data object GoogleSignUpClicked : SignUpIntent
    data object DismissError : SignUpIntent
    data object NavigateBack : SignUpIntent
}

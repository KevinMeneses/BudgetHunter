package com.meneses.budgethunter.auth.application

sealed interface SignInIntent {
    data class EmailChanged(val email: String) : SignInIntent
    data class PasswordChanged(val password: String) : SignInIntent
    data object SignInClicked : SignInIntent
    data object GoogleSignInClicked : SignInIntent
    data object DismissError : SignInIntent
    data object ContinueOfflineClicked : SignInIntent
}

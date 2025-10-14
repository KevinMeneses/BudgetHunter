package com.meneses.budgethunter.auth.application

import org.jetbrains.compose.resources.StringResource

data class SignInState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val isSignedIn: Boolean = false,
    val continueOffline: Boolean = false
)

package com.meneses.budgethunter.auth.application

import org.jetbrains.compose.resources.StringResource

data class SignInState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    /** False when the app was built without a Google client id, which hides the button. */
    val isGoogleAvailable: Boolean = false
)

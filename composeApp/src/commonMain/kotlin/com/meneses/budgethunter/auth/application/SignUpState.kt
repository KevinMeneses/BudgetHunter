package com.meneses.budgethunter.auth.application

import org.jetbrains.compose.resources.StringResource

data class SignUpState(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** A problem that belongs to no field (the server refused, Google failed...). */
    val error: StringResource? = null,
    val nameError: StringResource? = null,
    val emailError: StringResource? = null,
    val passwordError: StringResource? = null,
    /** False when the app was built without a Google client id, which hides the button. */
    val isGoogleAvailable: Boolean = false
)

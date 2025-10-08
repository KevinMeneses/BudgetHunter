package com.meneses.budgethunter.auth.application

import org.jetbrains.compose.resources.StringResource

data class SignUpState(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val isSignedUp: Boolean = false
)

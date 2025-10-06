package com.meneses.budgethunter.auth.application

data class SignUpState(
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignedUp: Boolean = false
)

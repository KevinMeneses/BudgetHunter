package com.meneses.budgethunter.auth.application

sealed interface SignUpEvent {
    data object NavigateToSignIn : SignUpEvent

    /** Google sign up completes the session outright, so there is no sign-in step to return to. */
    data class NavigateToBudgetList(val email: String) : SignUpEvent
}

package com.meneses.budgethunter.auth.application

sealed interface SignUpEvent {
    data object NavigateToSignIn : SignUpEvent
}

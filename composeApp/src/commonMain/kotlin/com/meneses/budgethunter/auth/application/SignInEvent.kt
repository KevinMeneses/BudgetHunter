package com.meneses.budgethunter.auth.application

sealed interface SignInEvent {
    data class NavigateToBudgetList(val email: String) : SignInEvent
}

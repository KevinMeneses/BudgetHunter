package com.meneses.budgethunter.auth.application

sealed interface SignInEvent {
    data object NavigateToBudgetList : SignInEvent
}

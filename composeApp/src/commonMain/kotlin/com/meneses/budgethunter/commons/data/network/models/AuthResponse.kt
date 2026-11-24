package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val authToken: String,
    val refreshToken: String,
    val email: String,
    val name: String,
    /**
     * Google sign in linked an account whose password nobody could vouch for, so the server
     * removed the password and ended every other session. Only sign in with Google sends it.
     */
    val passwordRemoved: Boolean = false,
    /** With [passwordRemoved]: the budgets the account already shared with other people. */
    val sharedBudgets: List<SharedBudgetResponse> = emptyList()
)

@Serializable
data class SharedBudgetResponse(
    val budgetId: Long,
    val name: String,
    val ownerEmail: String,
    val otherMembers: List<String>
)

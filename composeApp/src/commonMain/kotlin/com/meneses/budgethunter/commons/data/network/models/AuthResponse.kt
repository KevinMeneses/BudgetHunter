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

/**
 * Every field has a default: this arrives inside the sign in answer, and a field the server
 * dropped or renamed must not fail the sign in of exactly the users who need the notice.
 */
@Serializable
data class SharedBudgetResponse(
    val name: String = "",
    val ownerEmail: String = "",
    val otherMembers: List<String> = emptyList()
)

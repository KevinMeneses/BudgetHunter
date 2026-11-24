package com.meneses.budgethunter.auth.domain

import kotlinx.serialization.Serializable

/**
 * What the user has to know after Google sign in linked an account that had a password nobody
 * could vouch for (the server removed it and ended the other sessions, so whoever registered that
 * email first loses access).
 *
 * Kept on the device until the user dismisses it: the budget list shows it as a banner, and a
 * screen killed before the user read it shows it again on the next launch.
 */
@Serializable
data class GoogleLinkNotice(
    /** The account the notice is for, to tell the user's own budgets from other people's. */
    val accountEmail: String,
    /**
     * Budgets the account already shared with other people when it was linked. The server keeps
     * them, since telling a stranger's budget from a family one is something only the user can do.
     */
    val sharedBudgets: List<SharedBudget> = emptyList()
) {
    @Serializable
    data class SharedBudget(
        val name: String,
        val ownerEmail: String,
        val otherMembers: List<String>
    )
}

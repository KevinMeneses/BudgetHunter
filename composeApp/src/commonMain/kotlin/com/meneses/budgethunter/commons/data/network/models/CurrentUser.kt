package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

/**
 * The signed-in user's own profile, from `GET /api/users/me`.
 *
 * Kept apart from [UserInfo] — which is what the collaborators list returns — because
 * [hasPassword] describes how *this* account signs in and is nobody else's business.
 */
@Serializable
data class CurrentUser(
    val email: String,
    val name: String,
    /**
     * Whether the account can also be signed into with a password. Settings uses it to choose
     * between offering to set a first password and offering to change an existing one.
     */
    val hasPassword: Boolean = false
)

package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

/**
 * The settings preferences saved on the account, from `GET /api/users/me/preferences`.
 *
 * A null field means the account never saved it, which is how a fresh account is told apart from
 * one that chose a value.
 */
@Serializable
data class UserPreferencesResponse(
    val smsReadingEnabled: Boolean? = null,
    val aiProcessingEnabled: Boolean? = null,
    /** The budget's server-side id; the device's own ids mean nothing to other devices. */
    val defaultBudgetId: Long? = null,
    val selectedBankIds: List<String>? = null
)

@Serializable
data class UpdateUserPreferencesRequest(
    val smsReadingEnabled: Boolean,
    val aiProcessingEnabled: Boolean,
    val defaultBudgetId: Long? = null,
    val selectedBankIds: List<String> = emptyList()
)

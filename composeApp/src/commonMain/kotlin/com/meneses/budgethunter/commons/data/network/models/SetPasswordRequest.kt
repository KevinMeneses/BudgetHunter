package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class SetPasswordRequest(
    /** Null when the account has no password yet, which is the case for one created with Google. */
    val currentPassword: String? = null,
    val newPassword: String
)

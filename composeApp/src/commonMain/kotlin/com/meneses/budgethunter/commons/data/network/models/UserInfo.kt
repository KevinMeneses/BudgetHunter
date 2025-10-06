package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val email: String,
    val name: String
)

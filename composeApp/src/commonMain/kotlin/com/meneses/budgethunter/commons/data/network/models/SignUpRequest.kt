package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(
    val email: String,
    val name: String,
    val password: String,
    /** The privacy policy version accepted with the checkbox ([com.meneses.budgethunter.auth.domain.PrivacyPolicy.VERSION]). */
    val acceptedPrivacyPolicyVersion: String? = null
)

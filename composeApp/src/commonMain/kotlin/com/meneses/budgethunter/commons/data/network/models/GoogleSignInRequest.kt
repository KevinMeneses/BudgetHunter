package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSignInRequest(
    val idToken: String,
    /**
     * The privacy policy version the sign-in screen says continuing with Google accepts. The server
     * records it when it creates the account or when the account lacks the current version.
     */
    val acceptedPrivacyPolicyVersion: String? = null
)

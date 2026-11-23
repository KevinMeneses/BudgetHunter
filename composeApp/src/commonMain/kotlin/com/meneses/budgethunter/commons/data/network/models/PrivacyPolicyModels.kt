package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

/**
 * Whether the account accepted the privacy policy in force (`privacyPolicy` of `GET /api/users/me`).
 * [acceptedVersion] null = never accepted. The app asks for the acceptance when the two differ.
 */
@Serializable
data class PrivacyPolicyStatus(
    val currentVersion: String? = null,
    val acceptedVersion: String? = null
)

/** Body of `POST /api/users/me/privacy-policy`: only the server's current version is accepted (400 otherwise). */
@Serializable
data class AcceptPrivacyPolicyRequest(
    val version: String
)

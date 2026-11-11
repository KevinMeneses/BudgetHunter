package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

/** `GET /api/billing/status` and `POST /api/billing/sync`. */
@Serializable
data class BillingStatusResponse(
    /** The account's RevenueCat app user id (random, not the email). */
    val appUserId: String,
    /** FREE or PREMIUM. */
    val plan: String,
    val premiumUntil: String? = null
)

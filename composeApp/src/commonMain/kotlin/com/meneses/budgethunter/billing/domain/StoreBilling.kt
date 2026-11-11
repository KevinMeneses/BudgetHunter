package com.meneses.budgethunter.billing.domain

/**
 * The store side of a premium purchase (Google Play through RevenueCat on Android).
 *
 * It only buys and restores. Whether the account *is* premium is the backend's decision (`users.plan`, from
 * RevenueCat's webhook or `POST /api/billing/sync`); the app never unlocks anything from what the store says.
 */
interface StoreBilling {
    /** False when the build has no RevenueCat key for this platform: there is nothing to buy then. */
    val isAvailable: Boolean

    /**
     * Ties the store purchases on this device to the account's RevenueCat id (`GET /api/billing/status`).
     * Called before every offer list, purchase and restore, so a purchase never lands on another account.
     */
    suspend fun logIn(appUserId: String)

    /** Forgets the account on sign out. Never throws. */
    suspend fun logOut()

    /** The premium plans for sale in the current offering, empty when none is set up. */
    suspend fun offers(): List<PremiumOffer>

    /** @param offerId a [PremiumOffer.id] from the last [offers] call */
    suspend fun purchase(offerId: String): StorePurchaseResult

    /** Asks the store for this device's earlier purchases and hands them to the current account. */
    suspend fun restore()
}

enum class PremiumPeriod { MONTHLY, ANNUAL, OTHER }

/** One plan for sale. [price] is already formatted by the store in the user's currency. */
data class PremiumOffer(
    val id: String,
    val period: PremiumPeriod,
    val price: String,
    /** Length of the free trial, or null without one (or when the user already used it). */
    val freeTrialDays: Int? = null
)

enum class StorePurchaseResult {
    PURCHASED,

    /** The user closed the store's sheet. Not an error. */
    CANCELLED,

    /** Paid with a method that settles later (e.g. cash at a store); premium comes when it does. */
    PENDING
}

/** The store could not do it (not configured, no connection, product gone, store error). */
class StoreBillingException(message: String, cause: Throwable? = null) : Exception(message, cause)

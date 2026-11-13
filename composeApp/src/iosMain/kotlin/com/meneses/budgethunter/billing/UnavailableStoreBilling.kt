package com.meneses.budgethunter.billing

import com.meneses.budgethunter.billing.domain.PremiumOffer
import com.meneses.budgethunter.billing.domain.StoreBilling
import com.meneses.budgethunter.billing.domain.StoreBillingException
import com.meneses.budgethunter.billing.domain.StorePurchaseResult

/**
 * iOS sells nothing yet: Android launches first. The App Store side (RevenueCat's iOS SDK, an `appl_` key) comes
 * with the iOS launch; until then the paywall hides its buy buttons ([isAvailable] is false).
 */
class UnavailableStoreBilling : StoreBilling {
    override val isAvailable = false

    override suspend fun logIn(appUserId: String) = Unit

    override suspend fun logOut() = Unit

    override suspend fun offers(): List<PremiumOffer> = emptyList()

    override suspend fun purchase(offerId: String): StorePurchaseResult =
        throw StoreBillingException("Purchases are not available on iOS yet")

    override suspend fun restore() = throw StoreBillingException("Purchases are not available on iOS yet")
}

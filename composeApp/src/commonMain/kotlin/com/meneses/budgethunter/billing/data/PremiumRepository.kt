package com.meneses.budgethunter.billing.data

import com.meneses.budgethunter.billing.domain.BillingStatus
import com.meneses.budgethunter.billing.domain.PremiumOffer
import com.meneses.budgethunter.billing.domain.PurchaseOutcome
import com.meneses.budgethunter.billing.domain.AiUsage
import com.meneses.budgethunter.billing.domain.Allowance
import com.meneses.budgethunter.billing.domain.StoreBilling
import com.meneses.budgethunter.billing.domain.StorePurchaseResult
import com.meneses.budgethunter.commons.data.network.models.BillingStatusResponse
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Buying premium, end to end: the backend says who the account is at RevenueCat and what plan it has, the
 * store sells. Every store call is preceded by [StoreBilling.logIn] with the id the backend gives, so a
 * purchase can only land on the signed-in account, whatever was logged in before.
 *
 * Needs a signed-in account (every call goes to the backend first); offline mode cannot buy.
 */
class PremiumRepository(
    private val billingApi: BillingApiService,
    private val store: StoreBilling,
    private val logger: Logger
) {
    /** False when this build cannot sell (no RevenueCat key for the platform): hide the buy buttons. */
    val canPurchase: Boolean get() = store.isAvailable

    // One store flow at a time: the account is linked and then used, and a second flow in between (a double
    // tap, a reload) must not interleave its own link with this one's purchase.
    private val storeFlow = Mutex()

    /** @throws BillingApiException when the server cannot be asked */
    suspend fun status(): BillingStatus = billingApi.status().toStatus()

    /** @throws BillingApiException when the server cannot be asked */
    suspend fun aiUsage(): AiUsage = billingApi.aiUsage().let {
        AiUsage(
            receiptScans = allowance(it.receiptScansUsed, it.receiptScansLimit, it.receiptScansRemaining),
            categorizations = allowance(
                it.categorizedEntriesUsed,
                it.categorizedEntriesLimit,
                it.categorizedEntriesRemaining
            ),
            resetsOn = it.resetsOn
        )
    }

    private fun allowance(used: Int?, limit: Int?, remaining: Int?): Allowance? =
        if (used != null && limit != null && remaining != null) Allowance(used, limit, remaining) else null

    /**
     * @throws BillingApiException when the server cannot be asked
     * @throws com.meneses.budgethunter.billing.domain.StoreBillingException when the store cannot
     */
    suspend fun offers(): List<PremiumOffer> = storeFlow.withLock {
        linkAccount()
        store.offers()
    }

    /** @param offerId a [PremiumOffer.id] from [offers] */
    suspend fun purchase(offerId: String): PurchaseOutcome = storeFlow.withLock {
        linkAccount()
        when (store.purchase(offerId)) {
            StorePurchaseResult.PURCHASED -> PurchaseOutcome.Purchased(syncedStatus())
            StorePurchaseResult.CANCELLED -> PurchaseOutcome.Cancelled
            StorePurchaseResult.PENDING -> PurchaseOutcome.Pending
        }
    }

    /** Hands this device's earlier store purchases to the account, then answers the plan the server sees. */
    suspend fun restore(): BillingStatus = storeFlow.withLock {
        linkAccount()
        store.restore()
        syncedStatus() ?: status()
    }

    private suspend fun linkAccount() {
        store.logIn(billingApi.status().appUserId)
    }

    /** Null when the server could not be asked; the webhook still applies the purchase a moment later. */
    private suspend fun syncedStatus(): BillingStatus? = try {
        billingApi.sync().toStatus()
    } catch (e: CancellationException) {
        throw e
    } catch (e: BillingApiException) {
        logger.warn(TAG, "Bought, but the server could not sync the plan yet (HTTP ${e.status})", e)
        null
    }

    private fun BillingStatusResponse.toStatus() = BillingStatus(
        isPremium = plan.equals(PREMIUM, ignoreCase = true),
        premiumUntil = premiumUntil
    )

    private companion object {
        const val TAG = "PremiumRepository"
        const val PREMIUM = "PREMIUM"
    }
}

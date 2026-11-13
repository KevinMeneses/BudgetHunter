package com.meneses.budgethunter.billing

import android.app.Activity
import android.content.Context
import com.meneses.budgethunter.billing.domain.PremiumOffer
import com.meneses.budgethunter.billing.domain.PremiumPeriod
import com.meneses.budgethunter.billing.domain.StoreBilling
import com.meneses.budgethunter.billing.domain.StoreBillingException
import com.meneses.budgethunter.billing.domain.StorePurchaseResult
import com.meneses.budgethunter.commons.data.sync.Logger
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.models.Period
import kotlin.coroutines.cancellation.CancellationException

/** Supplies the [Activity] Google Play's purchase sheet needs (Koin only holds the application context). */
interface StoreActivityDelegate {
    fun currentActivity(): Activity?
}

/**
 * Google Play purchases through RevenueCat's Android SDK. Configured lazily, once, with the public `goog_` key;
 * without one ([isAvailable] false) nothing is configured and nothing can be bought.
 */
class AndroidStoreBilling(
    private val context: Context,
    private val apiKey: String,
    private val debugLogs: Boolean,
    private val logger: Logger
) : StoreBilling {

    @Volatile
    private var activityDelegate: StoreActivityDelegate? = null

    // The packages of the last offers() call, by id, so purchase() can hand the store the real object.
    // Replaced whole, never emptied in place, so a purchase during a reload still finds its package.
    @Volatile
    private var packages: Map<String, Package> = emptyMap()

    fun setActivityDelegate(delegate: StoreActivityDelegate) {
        activityDelegate = delegate
    }

    /**
     * Call from `onDestroy`: this is a singleton, and a delegate left behind would leak the Activity. Only clears
     * [delegate]'s own registration, so an Activity created before the old one is destroyed keeps its own.
     */
    fun clearActivityDelegate(delegate: StoreActivityDelegate) {
        if (activityDelegate === delegate) activityDelegate = null
    }

    override val isAvailable: Boolean get() = apiKey.isNotBlank()

    override suspend fun logIn(appUserId: String) {
        val purchases = purchases()
        if (purchases.appUserID == appUserId) return
        // Offers loaded for another account are not reused for this one
        packages = emptyMap()
        storeCall("log in") { purchases.awaitLogIn(appUserId) }
    }

    override suspend fun logOut() {
        if (!isAvailable || !Purchases.isConfigured) return
        val purchases = Purchases.sharedInstance
        // Logging out an anonymous user is an error in the SDK, and there is nothing to forget anyway
        if (purchases.isAnonymous) return
        try {
            purchases.awaitLogOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: PurchasesException) {
            logger.warn(TAG, "Could not log out of RevenueCat", e)
        }
        packages = emptyMap()
    }

    override suspend fun offers(): List<PremiumOffer> {
        val purchases = purchases()
        val offering = storeCall("load offerings") { purchases.awaitOfferings() }.current ?: return emptyList()
        packages = offering.availablePackages.associateBy { it.identifier }
        return offering.availablePackages.map { pkg ->
            PremiumOffer(
                id = pkg.identifier,
                period = when (pkg.packageType) {
                    PackageType.MONTHLY -> PremiumPeriod.MONTHLY
                    PackageType.ANNUAL -> PremiumPeriod.ANNUAL
                    else -> PremiumPeriod.OTHER
                },
                price = pkg.product.price.formatted,
                freeTrialDays = pkg.product.defaultOption?.freePhase?.billingPeriod?.let(::days)
            )
        }
    }

    override suspend fun purchase(offerId: String): StorePurchaseResult {
        val purchases = purchases()
        val pkg = packages[offerId] ?: throw StoreBillingException("Unknown offer $offerId; load the offers first")
        val activity = activityDelegate?.currentActivity()
            ?: throw StoreBillingException("No activity to show the purchase sheet")
        return try {
            purchases.awaitPurchase(PurchaseParams.Builder(activity, pkg).build())
            StorePurchaseResult.PURCHASED
        } catch (e: CancellationException) {
            throw e
        } catch (e: PurchasesTransactionException) {
            when {
                e.userCancelled -> StorePurchaseResult.CANCELLED
                e.code == PurchasesErrorCode.PaymentPendingError -> StorePurchaseResult.PENDING
                else -> throw StoreBillingException("Purchase failed: ${e.code}", e)
            }
        }
    }

    override suspend fun restore() {
        val purchases = purchases()
        storeCall("restore") { purchases.awaitRestore() }
    }

    @Synchronized
    private fun purchases(): Purchases {
        if (!isAvailable) throw StoreBillingException("This build has no RevenueCat key")
        if (!Purchases.isConfigured) {
            if (debugLogs) Purchases.logLevel = LogLevel.DEBUG
            // No appUserID here: logIn() always sets the account's id before anything is bought
            Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
        }
        return Purchases.sharedInstance
    }

    private suspend fun <T> storeCall(what: String, call: suspend () -> T): T = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: PurchasesException) {
        logger.warn(TAG, "RevenueCat could not $what: ${e.code}", e)
        throw StoreBillingException("Could not $what: ${e.code}", e)
    }

    private fun days(period: Period): Int = when (period.unit) {
        Period.Unit.DAY -> period.value
        Period.Unit.WEEK -> period.value * DAYS_PER_WEEK
        Period.Unit.MONTH -> period.value * DAYS_PER_MONTH
        Period.Unit.YEAR -> period.value * DAYS_PER_YEAR
        Period.Unit.UNKNOWN -> period.value
    }

    private companion object {
        const val TAG = "StoreBilling"
        const val DAYS_PER_WEEK = 7
        const val DAYS_PER_MONTH = 30
        const val DAYS_PER_YEAR = 365
    }
}

package com.meneses.budgethunter.billing.application

import com.meneses.budgethunter.billing.domain.PremiumOffer
import com.meneses.budgethunter.billing.domain.ScanUsage
import org.jetbrains.compose.resources.StringResource

data class PremiumState(
    val isLoading: Boolean = true,
    /** The plan could not be read (no connection, offline mode, signed out): only a retry is offered. */
    val loadFailed: Boolean = false,
    val isPremium: Boolean = false,
    val premiumUntil: String? = null,
    val usage: ScanUsage? = null,
    val offers: List<PremiumOffer> = emptyList(),
    /** False when this build or platform cannot sell; the plans are then not shown. */
    val canPurchase: Boolean = false,
    /** The plans were wanted but the store could not list them. */
    val offersFailed: Boolean = false,
    /** Bought, but the server has not applied it yet: the plans stay hidden until it does. */
    val isAwaitingServer: Boolean = false,
    /** A purchase or a restore is in progress. */
    val isWorking: Boolean = false,
    /** One-off message for the snackbar, cleared with [PremiumIntent.DismissMessage]. */
    val message: StringResource? = null
)

sealed interface PremiumIntent {
    data object Load : PremiumIntent
    data class Purchase(val offerId: String) : PremiumIntent
    data object Restore : PremiumIntent
    data object DismissMessage : PremiumIntent
}

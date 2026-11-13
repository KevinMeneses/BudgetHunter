package com.meneses.budgethunter.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.premium_error_purchase
import budgethunter.composeapp.generated.resources.premium_error_restore
import budgethunter.composeapp.generated.resources.premium_pending
import budgethunter.composeapp.generated.resources.premium_processing
import budgethunter.composeapp.generated.resources.premium_restore_none
import budgethunter.composeapp.generated.resources.premium_restored
import budgethunter.composeapp.generated.resources.premium_welcome
import com.meneses.budgethunter.billing.application.PremiumIntent
import com.meneses.budgethunter.billing.application.PremiumState
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.billing.domain.BillingStatus
import com.meneses.budgethunter.billing.domain.PurchaseOutcome
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The paywall: what the account has (plan and this month's scans, from the server) and what it can buy (the
 * store's plans). Nothing here decides premium; after a purchase or a restore it shows what the server says.
 */
class PremiumViewModel(
    private val premiumRepository: PremiumRepository,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumState(canPurchase = premiumRepository.canPurchase))
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun sendIntent(intent: PremiumIntent) {
        when (intent) {
            PremiumIntent.Load -> load()
            is PremiumIntent.Purchase -> purchase(intent.offerId)
            PremiumIntent.Restore -> restore()
            PremiumIntent.DismissMessage -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val account = try {
                val usage = async { premiumRepository.scanUsage() }
                premiumRepository.status() to usage.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(TAG, "Could not read the plan", e)
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }
            val (status, usage) = account
            _uiState.update { it.copy(usage = usage).withStatus(status) }

            // The plans are only shown to whoever can buy them
            val wantsOffers = premiumRepository.canPurchase && !status.isPremium
            val offers = if (wantsOffers) loadOffers() else emptyList()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    offers = offers.orEmpty(),
                    offersFailed = wantsOffers && offers == null
                )
            }
        }
    }

    /** Null when the store could not list them. */
    private suspend fun loadOffers() = try {
        premiumRepository.offers()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn(TAG, "Could not load the plans", e)
        null
    }

    private fun purchase(offerId: String) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val message = try {
                when (val outcome = premiumRepository.purchase(offerId)) {
                    is PurchaseOutcome.Purchased -> {
                        val status = outcome.status
                        if (status != null) _uiState.update { it.withStatus(status) }
                        // Paid, but the server has not seen it yet: the webhook brings it in a moment
                        if (status?.isPremium == true) Res.string.premium_welcome else Res.string.premium_processing
                    }
                    PurchaseOutcome.Pending -> Res.string.premium_pending
                    PurchaseOutcome.Cancelled -> null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(TAG, "Purchase failed", e)
                Res.string.premium_error_purchase
            }
            _uiState.update { it.copy(isWorking = false, message = message) }
            if (message == Res.string.premium_welcome) refreshUsage()
        }
    }

    private fun restore() {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val message = try {
                val status = premiumRepository.restore()
                _uiState.update { it.withStatus(status) }
                if (status.isPremium) Res.string.premium_restored else Res.string.premium_restore_none
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(TAG, "Restore failed", e)
                Res.string.premium_error_restore
            }
            _uiState.update { it.copy(isWorking = false, message = message) }
            if (message == Res.string.premium_restored) refreshUsage()
        }
    }

    /** The premium allowance replaces the free one as soon as the plan changes. */
    private suspend fun refreshUsage() {
        try {
            val usage = premiumRepository.scanUsage()
            _uiState.update { it.copy(usage = usage) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not refresh the scans", e)
        }
    }

    private fun PremiumState.withStatus(status: BillingStatus) = copy(
        isPremium = status.isPremium,
        premiumUntil = status.premiumUntil,
        offers = if (status.isPremium) emptyList() else offers
    )

    private companion object {
        const val TAG = "PremiumViewModel"
    }
}

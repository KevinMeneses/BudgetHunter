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
import budgethunter.composeapp.generated.resources.premium_still_processing
import budgethunter.composeapp.generated.resources.premium_welcome
import com.meneses.budgethunter.billing.application.PremiumIntent
import com.meneses.budgethunter.billing.application.PremiumState
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.billing.domain.BillingStatus
import com.meneses.budgethunter.billing.domain.PurchaseOutcome
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

    private var loadJob: Job? = null
    private var awaitJob: Job? = null

    // Bumped whenever a purchase or a restore writes the plan, so a load that started before cannot put the
    // older plan back when it finishes
    private var planVersion = 0

    init {
        load()
    }

    fun sendIntent(intent: PremiumIntent) {
        when (intent) {
            PremiumIntent.Load -> load()
            is PremiumIntent.Purchase -> purchase(intent.offerId)
            PremiumIntent.Restore -> restore()
            PremiumIntent.CheckAgain -> checkAgain()
            PremiumIntent.DismissMessage -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun load() {
        // A retry replaces the load in flight rather than racing it
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        val version = planVersion
        loadJob = viewModelScope.launch {
            val account = try {
                // The usage is extra: without it the plan and the plans for sale still show. coroutineScope
                // keeps any failure inside this try instead of failing the whole job.
                coroutineScope {
                    val usage = async { usageOrNull() }
                    premiumRepository.status() to usage.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(TAG, "Could not read the plan", e)
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }
            val (status, usage) = account
            val isPremium = if (planVersion == version) {
                _uiState.update { state -> state.copy(usage = usage ?: state.usage).withStatus(status) }
                status.isPremium
            } else {
                _uiState.value.isPremium
            }

            // The plans are only shown to whoever can buy them, and not while a purchase is still on its way
            val wantsOffers = premiumRepository.canPurchase && !isPremium && !_uiState.value.isAwaitingServer
            val offers = if (wantsOffers) loadOffers() else emptyList()
            _uiState.update {
                // A purchase or restore that landed meanwhile owns the plan (and cleared the plans for sale)
                val stillCurrent = planVersion == version && !it.isAwaitingServer
                it.copy(
                    isLoading = false,
                    offers = if (stillCurrent) offers.orEmpty() else it.offers,
                    offersFailed = stillCurrent && wantsOffers && offers == null
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
            var nowPremium = false
            val message = try {
                when (val outcome = premiumRepository.purchase(offerId)) {
                    is PurchaseOutcome.Purchased -> {
                        val status = outcome.status
                        if (status != null) applyPlan(status)
                        nowPremium = status?.isPremium == true
                        // Paid, but the server has not seen it yet: the webhook brings it in a moment. The plans are
                        // hidden meanwhile (nothing to buy twice) and the screen asks again shortly.
                        if (!nowPremium) awaitServer()
                        if (nowPremium) Res.string.premium_welcome else Res.string.premium_processing
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
            if (nowPremium) refreshUsage()
        }
    }

    private fun restore() {
        if (_uiState.value.isWorking) return
        awaitJob?.cancel()
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            var nowPremium = false
            val message = try {
                val status = premiumRepository.restore()
                applyPlan(status)
                nowPremium = status.isPremium
                if (nowPremium) Res.string.premium_restored else Res.string.premium_restore_none
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(TAG, "Restore failed", e)
                Res.string.premium_error_restore
            }
            _uiState.update { it.copy(isWorking = false, isAwaitingServer = false, message = message) }
            if (nowPremium) refreshUsage()
        }
    }

    /**
     * Paid, but the server has not applied it yet. The plans stay hidden (nothing to buy twice) while the plan is
     * asked again a few times, further apart each time; leaving the screen or a restore stops it. If the webhook
     * is slower still, the notice stays until the screen is opened again.
     */
    private fun awaitServer() {
        awaitJob?.cancel()
        _uiState.update { it.copy(isAwaitingServer = true, offers = emptyList()) }
        awaitJob = viewModelScope.launch {
            for (wait in SERVER_RECHECK_MILLIS) {
                delay(wait)
                val status = try {
                    premiumRepository.status()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.warn(TAG, "Could not recheck the plan", e)
                    continue
                }
                if (status.isPremium) {
                    applyPlan(status)
                    _uiState.update { it.copy(isAwaitingServer = false, message = Res.string.premium_welcome) }
                    refreshUsage()
                    return@launch
                }
            }
        }
    }

    /** The user asks, from the "arrives in a few minutes" notice, whether the purchase has reached the server. */
    private fun checkAgain() {
        if (_uiState.value.isWorking || !_uiState.value.isAwaitingServer) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val status = try {
                premiumRepository.status()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(TAG, "Could not check the plan again", e)
                null
            }
            if (status?.isPremium == true) {
                awaitJob?.cancel()
                applyPlan(status)
                _uiState.update { it.copy(isWorking = false, isAwaitingServer = false, message = Res.string.premium_welcome) }
                refreshUsage()
            } else {
                _uiState.update { it.copy(isWorking = false, message = Res.string.premium_still_processing) }
            }
        }
    }

    private suspend fun usageOrNull() = try {
        premiumRepository.aiUsage()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn(TAG, "Could not read the scans", e)
        null
    }

    /** The premium allowance replaces the free one as soon as the plan changes. */
    private suspend fun refreshUsage() {
        try {
            val usage = premiumRepository.aiUsage()
            _uiState.update { it.copy(usage = usage) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not refresh the scans", e)
        }
    }

    private fun applyPlan(status: BillingStatus) {
        planVersion++
        _uiState.update { it.withStatus(status) }
    }

    private fun PremiumState.withStatus(status: BillingStatus) = copy(
        isPremium = status.isPremium,
        premiumUntil = status.premiumUntil,
        offers = if (status.isPremium) emptyList() else offers
    )

    private companion object {
        const val TAG = "PremiumViewModel"

        // The webhook usually lands within seconds of a purchase; asked again after these waits
        val SERVER_RECHECK_MILLIS = listOf(5_000L, 15_000L, 30_000L, 60_000L)
    }
}

package com.meneses.budgethunter.billing.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.billing.domain.Allowance
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlin.coroutines.cancellation.CancellationException

/**
 * This month's receipt scans as the server counts them (`GET /api/ai/usage`), for showing how many are left.
 * Only informative: the server decides whether a scan goes through, the app never stops one on its own.
 */
class GetReceiptScansUseCase(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    private val premiumRepository: PremiumRepository,
    private val logger: Logger
) {
    /**
     * Null when there is nothing to show: offline mode or no account (receipts need one), the server cannot be
     * asked, or receipt reading is off on the server.
     */
    suspend fun execute(): Allowance? {
        if (preferencesManager.isOfflineModeEnabled() || !authRepository.isAuthenticated()) return null
        return try {
            premiumRepository.aiUsage().receiptScans
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not read this month's receipt scans", e)
            null
        }
    }

    private companion object {
        const val TAG = "GetReceiptScansUseCase"
    }
}

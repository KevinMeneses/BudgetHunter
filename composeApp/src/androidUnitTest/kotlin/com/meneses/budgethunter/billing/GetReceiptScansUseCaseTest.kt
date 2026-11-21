package com.meneses.budgethunter.billing

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.billing.application.GetReceiptScansUseCase
import com.meneses.budgethunter.billing.data.BillingApiException
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.billing.domain.AiUsage
import com.meneses.budgethunter.billing.domain.Allowance
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetReceiptScansUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val premiumRepository = mockk<PremiumRepository>()
    private val useCase = GetReceiptScansUseCase(authRepository, preferencesManager, premiumRepository, NoOpLogger())

    private val scans = Allowance(used = 8, limit = 10, remaining = 2)

    private fun signedIn(offline: Boolean = false, authenticated: Boolean = true) {
        coEvery { preferencesManager.isOfflineModeEnabled() } returns offline
        coEvery { authRepository.isAuthenticated() } returns authenticated
    }

    @Test
    fun `answers the server's receipt scans`() = runTest {
        signedIn()
        coEvery { premiumRepository.aiUsage() } returns AiUsage(scans, null, "2026-12-01")

        assertEquals(scans, useCase.execute())
    }

    @Test
    fun `offline mode asks nothing`() = runTest {
        signedIn(offline = true)

        assertNull(useCase.execute())
        coVerify(exactly = 0) { premiumRepository.aiUsage() }
    }

    @Test
    fun `no account asks nothing`() = runTest {
        signedIn(authenticated = false)

        assertNull(useCase.execute())
        coVerify(exactly = 0) { premiumRepository.aiUsage() }
    }

    @Test
    fun `a server error or no connection shows nothing`() = runTest {
        signedIn()
        coEvery { premiumRepository.aiUsage() } throws BillingApiException(500, "boom")
        assertNull(useCase.execute())

        coEvery { premiumRepository.aiUsage() } throws IllegalStateException("no connection")
        assertNull(useCase.execute())
    }

    @Test
    fun `running low is 3 or fewer whatever the limit`() {
        assertFalse(Allowance(used = 6, limit = 10, remaining = 4).isRunningLow)
        assertTrue(Allowance(used = 7, limit = 10, remaining = 3).isRunningLow)
        assertTrue(Allowance(used = 10, limit = 10, remaining = 0).isRunningLow)
        assertFalse(Allowance(used = 800, limit = 1000, remaining = 200).isRunningLow)
        assertTrue(Allowance(used = 997, limit = 1000, remaining = 3).isRunningLow)
    }
}

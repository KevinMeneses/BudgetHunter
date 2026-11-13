package com.meneses.budgethunter.billing

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.premium_error_purchase
import budgethunter.composeapp.generated.resources.premium_pending
import budgethunter.composeapp.generated.resources.premium_processing
import budgethunter.composeapp.generated.resources.premium_restore_none
import budgethunter.composeapp.generated.resources.premium_welcome
import com.meneses.budgethunter.billing.application.PremiumIntent
import com.meneses.budgethunter.billing.data.BillingApiException
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.billing.domain.BillingStatus
import com.meneses.budgethunter.billing.domain.PremiumOffer
import com.meneses.budgethunter.billing.domain.PremiumPeriod
import com.meneses.budgethunter.billing.domain.PurchaseOutcome
import com.meneses.budgethunter.billing.domain.ScanUsage
import com.meneses.budgethunter.billing.domain.StoreBillingException
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<PremiumRepository>()

    private val free = BillingStatus(isPremium = false, premiumUntil = null)
    private val premium = BillingStatus(isPremium = true, premiumUntil = "2026-12-07T10:00:00Z")
    private val freeUsage = ScanUsage(used = 10, limit = 10, remaining = 0, resetsOn = "2026-12-01")
    private val premiumUsage = ScanUsage(used = 10, limit = 200, remaining = 190, resetsOn = "2026-12-01")
    private val annual = PremiumOffer("\$rc_annual", PremiumPeriod.ANNUAL, "COP 59.900", freeTrialDays = 7)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.canPurchase } returns true
        coEvery { repository.status() } returns free
        coEvery { repository.scanUsage() } returns freeUsage
        coEvery { repository.offers() } returns listOf(annual)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PremiumViewModel(repository, NoOpLogger())

    @Test
    fun `a free account sees its scans and the plans`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isPremium)
        assertEquals(freeUsage, state.usage)
        assertEquals(listOf(annual), state.offers)
    }

    @Test
    fun `a premium account is not offered the plans`() = runTest(testDispatcher) {
        coEvery { repository.status() } returns premium

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPremium)
        assertEquals(emptyList(), viewModel.uiState.value.offers)
        coVerify(exactly = 0) { repository.offers() }
    }

    @Test
    fun `without the server only a retry is offered`() = runTest(testDispatcher) {
        coEvery { repository.status() } throws BillingApiException(null, "offline")

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loadFailed)

        coEvery { repository.status() } returns free
        viewModel.sendIntent(PremiumIntent.Load)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.loadFailed)
    }

    @Test
    fun `a failing usage call still shows the plan and the plans for sale`() = runTest(testDispatcher) {
        coEvery { repository.scanUsage() } throws BillingApiException(503, "HTTP 503")

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loadFailed)
        assertNull(state.usage)
        assertEquals(listOf(annual), state.offers)
    }

    @Test
    fun `a purchase the server has not applied hides the plans and asks again`() = runTest(testDispatcher) {
        coEvery { repository.purchase(any()) } returns PurchaseOutcome.Purchased(null)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendIntent(PremiumIntent.Purchase("x"))
        runCurrent()
        assertTrue(viewModel.uiState.value.isAwaitingServer)
        assertEquals(emptyList(), viewModel.uiState.value.offers)

        coEvery { repository.status() } returns premium
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAwaitingServer)
        assertTrue(viewModel.uiState.value.isPremium)
    }

    @Test
    fun `a load that finishes after a purchase does not bring the free plan back`() = runTest(testDispatcher) {
        coEvery { repository.purchase(any()) } returns PurchaseOutcome.Purchased(premium)
        val slowStatus = CompletableDeferred<BillingStatus>()
        val viewModel = viewModel()
        advanceUntilIdle()

        coEvery { repository.status() } coAnswers { slowStatus.await() }
        viewModel.sendIntent(PremiumIntent.Load)
        runCurrent()
        viewModel.sendIntent(PremiumIntent.Purchase("x"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPremium)

        slowStatus.complete(free)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPremium)
    }

    @Test
    fun `a store that cannot list the plans is told apart from a build that cannot sell`() = runTest(testDispatcher) {
        coEvery { repository.offers() } throws StoreBillingException("no network")
        val failing = viewModel()
        advanceUntilIdle()
        assertTrue(failing.uiState.value.offersFailed)

        every { repository.canPurchase } returns false
        val unavailable = viewModel()
        advanceUntilIdle()
        assertFalse(unavailable.uiState.value.canPurchase)
        assertFalse(unavailable.uiState.value.offersFailed)
    }

    @Test
    fun `a purchase the server confirms makes the screen premium with the new allowance`() = runTest(testDispatcher) {
        coEvery { repository.purchase("\$rc_annual") } returns PurchaseOutcome.Purchased(premium)
        val viewModel = viewModel()
        advanceUntilIdle()
        coEvery { repository.scanUsage() } returns premiumUsage

        viewModel.sendIntent(PremiumIntent.Purchase("\$rc_annual"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isPremium)
        assertEquals(premiumUsage, state.usage)
        assertEquals(Res.string.premium_welcome, state.message)
        assertFalse(state.isWorking)
    }

    @Test
    fun `each purchase outcome has its message, a cancel has none`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val cases = listOf(
            PurchaseOutcome.Purchased(null) to Res.string.premium_processing,
            PurchaseOutcome.Pending to Res.string.premium_pending,
            PurchaseOutcome.Cancelled to null
        )
        for ((outcome, message) in cases) {
            coEvery { repository.purchase(any()) } returns outcome
            viewModel.sendIntent(PremiumIntent.Purchase("x"))
            advanceUntilIdle()
            assertEquals(message, viewModel.uiState.value.message)
            viewModel.sendIntent(PremiumIntent.DismissMessage)
        }

        coEvery { repository.purchase(any()) } throws StoreBillingException("store error")
        viewModel.sendIntent(PremiumIntent.Purchase("x"))
        advanceUntilIdle()
        assertEquals(Res.string.premium_error_purchase, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isPremium)
    }

    @Test
    fun `restoring with nothing to restore says so`() = runTest(testDispatcher) {
        coEvery { repository.restore() } returns free
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendIntent(PremiumIntent.Restore)
        advanceUntilIdle()

        assertEquals(Res.string.premium_restore_none, viewModel.uiState.value.message)
        viewModel.sendIntent(PremiumIntent.DismissMessage)
        assertNull(viewModel.uiState.value.message)
    }
}

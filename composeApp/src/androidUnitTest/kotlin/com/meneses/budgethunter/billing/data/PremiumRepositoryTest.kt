package com.meneses.budgethunter.billing.data

import com.meneses.budgethunter.billing.domain.BillingStatus
import com.meneses.budgethunter.billing.domain.PremiumOffer
import com.meneses.budgethunter.billing.domain.PremiumPeriod
import com.meneses.budgethunter.billing.domain.PurchaseOutcome
import com.meneses.budgethunter.billing.domain.Allowance
import com.meneses.budgethunter.billing.domain.StoreBilling
import com.meneses.budgethunter.billing.domain.StoreBillingException
import com.meneses.budgethunter.billing.domain.StorePurchaseResult
import com.meneses.budgethunter.commons.data.network.models.AiUsageOverviewResponse
import com.meneses.budgethunter.commons.data.network.models.BillingStatusResponse
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class PremiumRepositoryTest {

    private val api = mockk<BillingApiService>()
    private val store = mockk<StoreBilling>(relaxed = true)
    private val repository = PremiumRepository(api, store, NoOpLogger())

    private val free = BillingStatusResponse(appUserId = "abc-123", plan = "FREE")
    private val premium = BillingStatusResponse("abc-123", "PREMIUM", "2026-12-07T10:00:00Z")

    init {
        coEvery { api.status() } returns free
        coEvery { api.sync() } returns premium
    }

    @Test
    fun `offers log the store in with the account's id first`() = runTest {
        val monthly = PremiumOffer("\$rc_monthly", PremiumPeriod.MONTHLY, "COP 9.900", freeTrialDays = 7)
        coEvery { store.offers() } returns listOf(monthly)

        assertEquals(listOf(monthly), repository.offers())
        coVerifyOrder {
            api.status()
            store.logIn("abc-123")
            store.offers()
        }
    }

    @Test
    fun `a purchase logs in, buys, then asks the server to sync`() = runTest {
        coEvery { store.purchase("annual") } returns StorePurchaseResult.PURCHASED

        val outcome = repository.purchase("annual")

        assertEquals(PurchaseOutcome.Purchased(BillingStatus(true, "2026-12-07T10:00:00Z")), outcome)
        coVerifyOrder {
            store.logIn("abc-123")
            store.purchase("annual")
            api.sync()
        }
    }

    @Test
    fun `a purchase the server cannot sync yet is still a purchase`() = runTest {
        coEvery { store.purchase("annual") } returns StorePurchaseResult.PURCHASED
        coEvery { api.sync() } throws BillingApiException(503, "HTTP 503")

        assertEquals(PurchaseOutcome.Purchased(null), repository.purchase("annual"))
    }

    @Test
    fun `cancelled and pending purchases do not sync`() = runTest {
        coEvery { store.purchase("a") } returns StorePurchaseResult.CANCELLED
        coEvery { store.purchase("b") } returns StorePurchaseResult.PENDING

        assertEquals(PurchaseOutcome.Cancelled, repository.purchase("a"))
        assertEquals(PurchaseOutcome.Pending, repository.purchase("b"))
        coVerify(exactly = 0) { api.sync() }
    }

    @Test
    fun `restore syncs, and falls back to the plain status when sync is refused`() = runTest {
        assertEquals(BillingStatus(true, "2026-12-07T10:00:00Z"), repository.restore())
        coVerifyOrder {
            store.logIn("abc-123")
            store.restore()
            api.sync()
        }

        coEvery { api.sync() } throws BillingApiException(429, "HTTP 429")
        assertEquals(BillingStatus(false, null), repository.restore())
    }

    @Test
    fun `nothing is bought when the store cannot log in with the account's id`() = runTest {
        coEvery { store.logIn("abc-123") } throws StoreBillingException("Could not log in")

        assertFailsWith<StoreBillingException> { repository.purchase("annual") }
        coVerify(exactly = 0) { store.purchase(any()) }
    }

    @Test
    fun `a restore the store fails is reported and not synced`() = runTest {
        coEvery { store.restore() } throws StoreBillingException("Could not restore")

        assertFailsWith<StoreBillingException> { repository.restore() }
        coVerify(exactly = 0) { api.sync() }
    }

    @Test
    fun `without the server the store is never touched`() = runTest {
        coEvery { api.status() } throws BillingApiException(null, "offline")

        assertFailsWith<BillingApiException> { repository.purchase("annual") }
        assertFailsWith<BillingApiException> { repository.offers() }
        coVerify(exactly = 0) { store.logIn(any()) }
    }

    @Test
    fun `ai usage reads both allowances, and a feature off on the server has none`() = runTest {
        coEvery { api.aiUsage() } returns AiUsageOverviewResponse(
            plan = "FREE",
            period = "2026-11",
            receiptScansUsed = 3,
            receiptScansLimit = 10,
            receiptScansRemaining = 7,
            resetsOn = "2026-12-01"
        )

        val usage = repository.aiUsage()

        assertEquals(Allowance(used = 3, limit = 10, remaining = 7), usage.receiptScans)
        assertNull(usage.categorizations)
        assertEquals("2026-12-01", usage.resetsOn)
    }

    @Test
    fun `canPurchase follows the store`() {
        every { store.isAvailable } returns false
        assertFalse(repository.canPurchase)
    }
}

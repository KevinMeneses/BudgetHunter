package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.auth.data.AuthSession
import com.meneses.budgethunter.auth.data.DeleteAccountResult
import com.meneses.budgethunter.billing.data.BillingApiException
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.billing.domain.BillingStatus
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteAccountUseCaseTest {

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val premiumRepository = mockk<PremiumRepository>(relaxed = true)
    private val signOutUseCase = mockk<SignOutUseCase>(relaxed = true)
    private val authSession = AuthSession()

    private val useCase = DeleteAccountUseCase(
        authRepository = authRepository,
        premiumRepository = premiumRepository,
        signOutUseCase = signOutUseCase,
        authSession = authSession,
        logger = NoOpLogger(),
        ioDispatcher = Dispatchers.Unconfined,
        planTimeoutMillis = 1_000
    )

    @Test
    fun `a deleted account signs out, which clears the device`() = runTest {
        coEvery { authRepository.deleteAccount() } returns DeleteAccountResult.DELETED

        val result = useCase.execute()

        assertEquals(DeleteAccountUseCase.Result.DELETED, result)
        coVerify(exactly = 1) { signOutUseCase.execute() }
    }

    @Test
    fun `the token is renewed through the usual client before the delete`() = runTest {
        coEvery { authRepository.deleteAccount() } returns DeleteAccountResult.DELETED

        useCase.execute()

        coVerifyOrder {
            authRepository.getCurrentUser()
            authRepository.deleteAccount()
        }
    }

    @Test
    fun `a deleted account still leaves for sign in when signing out fails`() = runTest {
        coEvery { authRepository.deleteAccount() } returns DeleteAccountResult.DELETED
        coEvery { signOutUseCase.execute() } throws RuntimeException("keystore unavailable")

        val result = useCase.execute()

        assertEquals(DeleteAccountUseCase.Result.DELETED, result)
        assertTrue(authSession.isSignInRedirectPending.value)
        // Not an expiry: the user asked for this
        assertFalse(authSession.consumeSessionExpiredNotice())
    }

    @Test
    fun `a refused session drops the tokens and goes to sign in as expired, keeping the data`() = runTest {
        coEvery { authRepository.deleteAccount() } returns DeleteAccountResult.UNAUTHORIZED

        val result = useCase.execute()

        assertEquals(DeleteAccountUseCase.Result.SESSION_EXPIRED, result)
        coVerify(exactly = 1) { authRepository.signOut() }
        coVerify(exactly = 0) { signOutUseCase.execute() }
        assertTrue(authSession.isSignInRedirectPending.value)
        assertTrue(authSession.consumeSessionExpiredNotice())
    }

    @Test
    fun `a failed request changes nothing`() = runTest {
        coEvery { authRepository.deleteAccount() } returns DeleteAccountResult.FAILED

        val result = useCase.execute()

        assertEquals(DeleteAccountUseCase.Result.FAILED, result)
        coVerify(exactly = 0) { signOutUseCase.execute() }
        coVerify(exactly = 0) { authRepository.signOut() }
        assertFalse(authSession.isSignInRedirectPending.value)
    }

    @Test
    fun `isPremium is true for a premium plan`() = runTest {
        coEvery { premiumRepository.status() } returns BillingStatus(isPremium = true, premiumUntil = "2026-12-07T10:00:00Z")

        assertEquals(true, useCase.isPremium())
    }

    @Test
    fun `isPremium is false for a free plan`() = runTest {
        coEvery { premiumRepository.status() } returns BillingStatus(isPremium = false, premiumUntil = null)

        assertEquals(false, useCase.isPremium())
    }

    @Test
    fun `isPremium is unknown when the plan cannot be read`() = runTest {
        coEvery { premiumRepository.status() } throws BillingApiException(null, "offline")

        assertNull(useCase.isPremium())
    }

    @Test
    fun `isPremium is unknown when the server takes too long`() = runTest {
        coEvery { premiumRepository.status() } coAnswers { awaitCancellation() }

        assertNull(useCase.isPremium())
    }
}

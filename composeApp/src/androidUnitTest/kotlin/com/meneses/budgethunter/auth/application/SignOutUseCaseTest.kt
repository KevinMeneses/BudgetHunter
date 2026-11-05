package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.auth.data.AuthSession
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import com.meneses.budgethunter.commons.platform.GoogleSignInManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignOutUseCaseTest {

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val authSession = AuthSession()
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val budgetEntryRepository = mockk<BudgetEntryRepository>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val googleSignInManager = mockk<GoogleSignInManager>(relaxed = true)

    private val useCase = SignOutUseCase(
        authRepository = authRepository,
        authSession = authSession,
        budgetRepository = budgetRepository,
        budgetEntryRepository = budgetEntryRepository,
        preferencesManager = preferencesManager,
        googleSignInManager = googleSignInManager,
        logger = NoOpLogger(),
        ioDispatcher = Dispatchers.Unconfined
    )

    @Test
    fun `execute ends the session before clearing the device`() = runTest {
        useCase.execute()

        coVerifyOrder {
            authRepository.signOut()
            preferencesManager.setOfflineModeEnabled(false)
            googleSignInManager.signOut()
            budgetRepository.clearAllData()
            budgetEntryRepository.clearAllData()
            preferencesManager.clearUserPreferences()
        }
    }

    @Test
    fun `a failing cleanup step does not stop the ones after it`() = runTest {
        coEvery { budgetRepository.clearAllData() } throws RuntimeException("disk I/O error")

        useCase.execute()

        coVerify(exactly = 1) { budgetEntryRepository.clearAllData() }
        coVerify(exactly = 1) { preferencesManager.clearUserPreferences() }
        assertTrue(authSession.isSignInRedirectPending.value)
    }

    @Test
    fun `execute marks the sign in redirect as pending when done`() = runTest {
        assertFalse(authSession.isSignInRedirectPending.value)

        useCase.execute()

        assertTrue(authSession.isSignInRedirectPending.value)
    }

    @Test
    fun `execute propagates the failure and clears nothing when the session cannot be ended`() = runTest {
        coEvery { authRepository.signOut() } throws RuntimeException("keystore unavailable")

        assertFailsWith<RuntimeException> { useCase.execute() }

        coVerify(exactly = 0) { googleSignInManager.signOut() }
        coVerify(exactly = 0) { budgetRepository.clearAllData() }
        coVerify(exactly = 0) { budgetEntryRepository.clearAllData() }
        coVerify(exactly = 0) { preferencesManager.clearUserPreferences() }
        assertFalse(authSession.isSignInRedirectPending.value)
    }

    @Test
    fun `execute does not throw and still redirects when clearing local data fails`() = runTest {
        coEvery { budgetRepository.clearAllData() } throws RuntimeException("disk full")

        useCase.execute()

        assertTrue(authSession.isSignInRedirectPending.value)
    }

    @Test
    fun `hasUnsyncedChanges is true when budgets have unsynced changes`() = runTest {
        coEvery { budgetRepository.hasUnsyncedChanges() } returns true
        coEvery { budgetEntryRepository.hasUnsyncedChanges() } returns false

        assertTrue(useCase.hasUnsyncedChanges())
    }

    @Test
    fun `hasUnsyncedChanges is true when entries have unsynced changes`() = runTest {
        coEvery { budgetRepository.hasUnsyncedChanges() } returns false
        coEvery { budgetEntryRepository.hasUnsyncedChanges() } returns true

        assertTrue(useCase.hasUnsyncedChanges())
    }

    @Test
    fun `hasUnsyncedChanges is false when nothing is unsynced`() = runTest {
        coEvery { budgetRepository.hasUnsyncedChanges() } returns false
        coEvery { budgetEntryRepository.hasUnsyncedChanges() } returns false

        assertFalse(useCase.hasUnsyncedChanges())
    }
}

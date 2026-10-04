package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PrepareDataForAccountUseCaseTest {

    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val useCase = PrepareDataForAccountUseCase(preferencesManager, budgetRepository)

    @Test
    fun `clears synced data and preferences when a different account signs in`() = runTest {
        coEvery { preferencesManager.getLastSignedInEmail() } returns "a@example.com"

        useCase.execute("b@example.com")

        coVerify(exactly = 1) { budgetRepository.clearSyncedData() }
        coVerify(exactly = 1) { preferencesManager.clearUserPreferences() }
        coVerify(exactly = 1) { preferencesManager.setLastSignedInEmail("b@example.com") }
    }

    @Test
    fun `stores the new email only after clearing`() = runTest {
        coEvery { preferencesManager.getLastSignedInEmail() } returns "a@example.com"

        useCase.execute("b@example.com")

        coVerifyOrder {
            budgetRepository.clearSyncedData()
            preferencesManager.clearUserPreferences()
            preferencesManager.setLastSignedInEmail("b@example.com")
        }
    }

    @Test
    fun `keeps everything when the same account signs in again`() = runTest {
        coEvery { preferencesManager.getLastSignedInEmail() } returns "a@example.com"

        useCase.execute("a@example.com")

        coVerify(exactly = 0) { budgetRepository.clearSyncedData() }
        coVerify(exactly = 0) { preferencesManager.clearUserPreferences() }
        coVerify(exactly = 1) { preferencesManager.setLastSignedInEmail("a@example.com") }
    }

    @Test
    fun `treats emails that differ only by case as the same account`() = runTest {
        coEvery { preferencesManager.getLastSignedInEmail() } returns "User@Example.com"

        useCase.execute("user@example.COM")

        coVerify(exactly = 0) { budgetRepository.clearSyncedData() }
        coVerify(exactly = 0) { preferencesManager.clearUserPreferences() }
    }

    @Test
    fun `treats emails that differ only by surrounding whitespace as the same account`() = runTest {
        coEvery { preferencesManager.getLastSignedInEmail() } returns " user@example.com "

        useCase.execute("user@example.com")

        coVerify(exactly = 0) { budgetRepository.clearSyncedData() }
        coVerify(exactly = 0) { preferencesManager.clearUserPreferences() }
    }

    @Test
    fun `does not clear anything on the first sign in`() = runTest {
        coEvery { preferencesManager.getLastSignedInEmail() } returns null

        useCase.execute("a@example.com")

        coVerify(exactly = 0) { budgetRepository.clearSyncedData() }
        coVerify(exactly = 0) { preferencesManager.clearUserPreferences() }
        coVerify(exactly = 1) { preferencesManager.setLastSignedInEmail("a@example.com") }
    }

    @Test
    fun `stores the email as received`() = runTest {
        coEvery { preferencesManager.getLastSignedInEmail() } returns null

        useCase.execute("Mixed.Case@Example.com")

        coVerify(exactly = 1) { preferencesManager.setLastSignedInEmail("Mixed.Case@Example.com") }
    }
}

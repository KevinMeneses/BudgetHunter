package com.meneses.budgethunter.settings

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.models.UpdateUserPreferencesRequest
import com.meneses.budgethunter.commons.data.network.models.UserPreferencesResponse
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import com.meneses.budgethunter.settings.data.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.BeforeTest
import kotlin.test.Test

class SyncUserPreferencesUseCaseTest {

    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>()
    private val budgetRepository = mockk<BudgetRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val logger = mockk<Logger>(relaxed = true)

    private val useCase = SyncUserPreferencesUseCase(
        preferencesManager,
        userPreferencesRepository,
        budgetRepository,
        authRepository,
        logger
    )

    @BeforeTest
    fun setup() {
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { preferencesManager.isSmsReadingEnabled() } returns true
        coEvery { preferencesManager.isAiProcessingEnabled() } returns false
        coEvery { preferencesManager.getSelectedBankIds() } returns setOf("nequi")
        coEvery { preferencesManager.getDefaultBudgetId() } returns 3
        coEvery { budgetRepository.getById(3) } returns Budget(id = 3, serverId = 30L)
        coEvery { budgetRepository.getAllCached() } returns listOf(
            Budget(id = 3, serverId = 30L),
            Budget(id = 8, serverId = 80L)
        )
    }

    @Test
    fun `push sends the device values with the default budget as its server id`() = runTest {
        coEvery { userPreferencesRepository.save(any()) } returns Result.success(UserPreferencesResponse())

        useCase.push()

        coVerify {
            userPreferencesRepository.save(
                UpdateUserPreferencesRequest(
                    smsReadingEnabled = true,
                    aiProcessingEnabled = false,
                    defaultBudgetId = 30L,
                    selectedBankIds = listOf("nequi")
                )
            )
        }
    }

    @Test
    fun `push does nothing when signed out`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns false

        useCase.push()

        coVerify(exactly = 0) { userPreferencesRepository.save(any()) }
    }

    @Test
    fun `push keeps a never-synced default budget local only`() = runTest {
        coEvery { budgetRepository.getById(3) } returns Budget(id = 3, serverId = null)
        coEvery { userPreferencesRepository.save(any()) } returns Result.success(UserPreferencesResponse())

        useCase.push()

        coVerify { userPreferencesRepository.save(match { it.defaultBudgetId == null }) }
    }

    @Test
    fun `pull applies the account's preferences and resolves the default budget locally`() = runTest {
        coEvery { userPreferencesRepository.get() } returns Result.success(
            UserPreferencesResponse(
                smsReadingEnabled = false,
                aiProcessingEnabled = true,
                defaultBudgetId = 80L,
                selectedBankIds = listOf("bancolombia")
            )
        )

        useCase.pull()

        coVerify {
            preferencesManager.setSmsReadingEnabled(false)
            preferencesManager.setAiProcessingEnabled(true)
            preferencesManager.setSelectedBankIds(setOf("bancolombia"))
            preferencesManager.setDefaultBudgetId(8)
        }
        coVerify(exactly = 0) { userPreferencesRepository.save(any()) }
    }

    @Test
    fun `pull clears the default budget when the device does not have it`() = runTest {
        coEvery { userPreferencesRepository.get() } returns Result.success(
            UserPreferencesResponse(smsReadingEnabled = true, aiProcessingEnabled = true, defaultBudgetId = 999L)
        )

        useCase.pull()

        coVerify { preferencesManager.setDefaultBudgetId(-1) }
    }

    @Test
    fun `pull uploads the device values to an account that never saved any`() = runTest {
        coEvery { userPreferencesRepository.get() } returns Result.success(UserPreferencesResponse())
        coEvery { userPreferencesRepository.save(any()) } returns Result.success(UserPreferencesResponse())

        useCase.pull()

        coVerify { userPreferencesRepository.save(any()) }
        coVerify(exactly = 0) { preferencesManager.setSmsReadingEnabled(any()) }
    }

    @Test
    fun `pull leaves the device untouched when the account cannot be reached`() = runTest {
        coEvery { userPreferencesRepository.get() } returns Result.failure(RuntimeException("offline"))

        useCase.pull()

        coVerify(exactly = 0) { preferencesManager.setSmsReadingEnabled(any()) }
        coVerify(exactly = 0) { userPreferencesRepository.save(any()) }
    }

    @Test
    fun `pull does not overwrite a change the user made while it was in flight`() = runTest {
        coEvery { userPreferencesRepository.save(any()) } returns Result.success(UserPreferencesResponse())
        coEvery { userPreferencesRepository.get() } coAnswers {
            // The user toggles something while the request is out.
            launch { useCase.push() }
            yield()
            Result.success(UserPreferencesResponse(smsReadingEnabled = false, aiProcessingEnabled = false))
        }

        useCase.pull()

        coVerify(exactly = 0) { preferencesManager.setSmsReadingEnabled(any()) }
        // ...and the user's value still reaches the account.
        coVerify(exactly = 1) { userPreferencesRepository.save(any()) }
    }
}

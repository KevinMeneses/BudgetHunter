package com.meneses.budgethunter.auth

import com.meneses.budgethunter.auth.application.SignInEvent
import com.meneses.budgethunter.auth.application.SignInIntent
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Unit tests for SignInViewModel event emissions.
 *
 * Verifies that the ViewModel emits a NavigateToBudgetList event upon successful
 * sign-in and when the user explicitly chooses to continue offline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val budgetEntrySyncManager = mockk<BudgetEntrySyncManager>(relaxed = true)

    // System under test
    private lateinit var viewModel: SignInViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SignInViewModel(
            authRepository = authRepository,
            preferencesManager = preferencesManager,
            budgetRepository = budgetRepository,
            budgetEntrySyncManager = budgetEntrySyncManager
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== signIn success Tests ==========

    @Test
    fun `signIn success emits NavigateToBudgetList event`() = runTest {
        // Given - valid credentials and successful API response
        val email = "user@example.com"
        val password = "securepass"
        val signInResponse = AuthResponse(
            authToken = "auth-token-abc",
            refreshToken = "refresh-token-xyz",
            email = email,
            name = "Test User"
        )
        viewModel.sendIntent(SignInIntent.EmailChanged(email))
        viewModel.sendIntent(SignInIntent.PasswordChanged(password))

        coEvery {
            authRepository.signIn(email = email, password = password)
        } returns Result.success(signInResponse)

        // When
        viewModel.sendIntent(SignInIntent.SignInClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - navigation to budget list is emitted
        val event = viewModel.events.first()
        assertIs<SignInEvent.NavigateToBudgetList>(event)
    }

    // ========== continueOffline Tests ==========

    @Test
    fun `continueOffline emits NavigateToBudgetList event`() = runTest {
        // Given - user wants to use the app offline
        coJustRun { preferencesManager.setOfflineModeEnabled(true) }

        // When
        viewModel.sendIntent(SignInIntent.ContinueOfflineClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - navigation to budget list is emitted even without authentication
        val event = viewModel.events.first()
        assertIs<SignInEvent.NavigateToBudgetList>(event)
    }
}

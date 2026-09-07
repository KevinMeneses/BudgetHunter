package com.meneses.budgethunter.auth

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.error_google_no_account
import budgethunter.composeapp.generated.resources.error_google_sign_in_failed
import com.meneses.budgethunter.auth.application.GoogleAuthOutcome
import com.meneses.budgethunter.auth.application.SignInEvent
import com.meneses.budgethunter.auth.application.SignInIntent
import com.meneses.budgethunter.auth.application.SignInWithGoogleUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import io.mockk.coEvery
import io.mockk.every
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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
    private val signInWithGoogleUseCase = mockk<SignInWithGoogleUseCase>(relaxed = true)

    // System under test
    private lateinit var viewModel: SignInViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { signInWithGoogleUseCase.isAvailable } returns true
        viewModel = SignInViewModel(
            authRepository = authRepository,
            preferencesManager = preferencesManager,
            budgetRepository = budgetRepository,
            budgetEntrySyncManager = budgetEntrySyncManager,
            signInWithGoogleUseCase = signInWithGoogleUseCase
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

        // Then - navigation to budget list is emitted with the authenticated user's email
        val event = viewModel.events.first()
        assertIs<SignInEvent.NavigateToBudgetList>(event)
        assertEquals(email, event.email)
    }

    // ========== continueOffline Tests ==========

    @Test
    fun `continueOffline emits NavigateToBudgetList event`() = runTest {
        // Given - user wants to use the app offline
        coJustRun { preferencesManager.setOfflineModeEnabled(true) }

        // When
        viewModel.sendIntent(SignInIntent.ContinueOfflineClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - navigation to budget list is emitted even without authentication, with a blank email
        val event = viewModel.events.first()
        assertIs<SignInEvent.NavigateToBudgetList>(event)
        assertTrue(event.email.isBlank(), "Email should be blank for offline mode")
    }

    // ========== Google sign in Tests ==========

    @Test
    fun `google sign in success emits NavigateToBudgetList with the server email`() = runTest {
        // Given - the user never typed an email, so it has to come from the response
        coEvery { signInWithGoogleUseCase.execute() } returns
            GoogleAuthOutcome.Success("google-user@example.com")

        // When
        viewModel.sendIntent(SignInIntent.GoogleSignInClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertIs<SignInEvent.NavigateToBudgetList>(event)
        assertEquals("google-user@example.com", event.email)
    }

    @Test
    fun `google sign in cancellation clears loading without an error`() = runTest {
        // Given - dismissing the account picker is the most common outcome of this flow
        coEvery { signInWithGoogleUseCase.execute() } returns GoogleAuthOutcome.Cancelled

        // When
        viewModel.sendIntent(SignInIntent.GoogleSignInClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - showing a red error card here would make the screen feel broken
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `google sign in without a device account reports a distinct error`() = runTest {
        // Given
        coEvery { signInWithGoogleUseCase.execute() } returns GoogleAuthOutcome.NoGoogleAccount

        // When
        viewModel.sendIntent(SignInIntent.GoogleSignInClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - "add a Google account" is actionable in a way "sign in failed" is not
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(Res.string.error_google_no_account, state.error)
    }

    @Test
    fun `google sign in rejected by the backend surfaces a failure`() = runTest {
        // Given
        coEvery { signInWithGoogleUseCase.execute() } returns GoogleAuthOutcome.Failed

        // When
        viewModel.sendIntent(SignInIntent.GoogleSignInClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(Res.string.error_google_sign_in_failed, state.error)
    }

    @Test
    fun `google button is hidden when no client id was configured`() {
        // Given - a build without GOOGLE_SERVER_CLIENT_ID
        every { signInWithGoogleUseCase.isAvailable } returns false

        // When
        val viewModel = SignInViewModel(
            authRepository = authRepository,
            preferencesManager = preferencesManager,
            budgetRepository = budgetRepository,
            budgetEntrySyncManager = budgetEntrySyncManager,
            signInWithGoogleUseCase = signInWithGoogleUseCase
        )

        // Then
        assertEquals(false, viewModel.uiState.value.isGoogleAvailable)
    }
}

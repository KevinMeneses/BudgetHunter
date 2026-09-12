package com.meneses.budgethunter.auth

import com.meneses.budgethunter.auth.application.GoogleAuthOutcome
import com.meneses.budgethunter.auth.application.SignInWithGoogleUseCase
import com.meneses.budgethunter.auth.application.SignUpEvent
import com.meneses.budgethunter.auth.application.SignUpIntent
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.network.models.SignUpResponse
import io.mockk.coEvery
import io.mockk.every
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
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for SignUpViewModel event emissions.
 *
 * Verifies that the ViewModel emits a NavigateToSignIn event after a successful
 * account registration, prompting the user to proceed to the sign-in screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val signInWithGoogleUseCase = mockk<SignInWithGoogleUseCase>(relaxed = true)

    // System under test
    private lateinit var viewModel: SignUpViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { signInWithGoogleUseCase.isAvailable } returns true
        viewModel = SignUpViewModel(
            authRepository = authRepository,
            signInWithGoogleUseCase = signInWithGoogleUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== signUp success Tests ==========

    @Test
    fun `signUp success emits NavigateToSignIn event`() = runTest {
        // Given - all required fields are filled with valid values
        val email = "newuser@example.com"
        val name = "New User"
        val password = "password123"

        viewModel.sendIntent(SignUpIntent.EmailChanged(email))
        viewModel.sendIntent(SignUpIntent.NameChanged(name))
        viewModel.sendIntent(SignUpIntent.PasswordChanged(password))
        viewModel.sendIntent(SignUpIntent.ConfirmPasswordChanged(password))

        // API call succeeds and returns the registered user info
        val userResponse = SignUpResponse(email = email, name = name)
        coEvery {
            authRepository.signUp(email = email, name = name, password = password)
        } returns Result.success(userResponse)

        // When
        viewModel.sendIntent(SignUpIntent.SignUpClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - the user is redirected to sign-in to authenticate with the new account
        val event = viewModel.events.first()
        assertIs<SignUpEvent.NavigateToSignIn>(event)
    }

    // ========== Google sign up Tests ==========

    @Test
    fun `google sign up success emits NavigateToBudgetList event`() = runTest {
        // Given - the server creates the account and returns a session
        coEvery { signInWithGoogleUseCase.execute() } returns
            GoogleAuthOutcome.Success("google-user@example.com")

        // When
        viewModel.sendIntent(SignUpIntent.GoogleSignUpClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - signing up with Google leaves the user signed in, so there is no sign-in step
        val event = viewModel.events.first()
        assertIs<SignUpEvent.NavigateToBudgetList>(event)
        assertEquals("google-user@example.com", event.email)
    }

    @Test
    fun `google sign up cancellation clears loading without an error`() = runTest {
        // Given - the user dismisses the account picker
        coEvery { signInWithGoogleUseCase.execute() } returns GoogleAuthOutcome.Cancelled

        // When
        viewModel.sendIntent(SignUpIntent.GoogleSignUpClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - backing out is not a failure and must not show one
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }
}

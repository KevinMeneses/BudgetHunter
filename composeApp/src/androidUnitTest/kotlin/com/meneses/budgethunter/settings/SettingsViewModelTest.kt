package com.meneses.budgethunter.settings

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.sign_out_failed
import com.meneses.budgethunter.auth.application.SignOutUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.models.CurrentUser
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.application.SettingsEvent
import com.meneses.budgethunter.settings.application.SettingsIntent
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsViewModel: the sign out flow and the account section.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val permissionsManager = mockk<PermissionsManager>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val syncUserPreferences = mockk<SyncUserPreferencesUseCase>(relaxed = true)
    private val signOutUseCase = mockk<SignOutUseCase>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { preferencesManager.getDefaultBudgetId() } returns -1
        coEvery { preferencesManager.getSelectedBankIds() } returns emptySet()
        coEvery { authRepository.isAuthenticated() } returns false
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(
        preferencesManager = preferencesManager,
        budgetRepository = budgetRepository,
        permissionsManager = permissionsManager,
        authRepository = authRepository,
        syncUserPreferences = syncUserPreferences,
        signOutUseCase = signOutUseCase,
        applicationScope = CoroutineScope(testDispatcher)
    )

    @Test
    fun `ToggleSignOutConfirmation true shows the confirmation`() = runTest {
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))

        assertTrue(viewModel.uiState.value.isSignOutConfirmationVisible)
    }

    @Test
    fun `ToggleSignOutConfirmation false hides the confirmation`() = runTest {
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(false))

        assertFalse(viewModel.uiState.value.isSignOutConfirmationVisible)
    }

    @Test
    fun `SignOut success emits NavigateToSignIn and closes the confirmation`() = runTest {
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))

        viewModel.sendIntent(SettingsIntent.SignOut)
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<SettingsEvent.NavigateToSignIn>(viewModel.events.first())
        val state = viewModel.uiState.value
        assertFalse(state.isSignOutConfirmationVisible)
        assertFalse(state.isSigningOut)
        assertNull(state.signOutError)
    }

    @Test
    fun `SignOut failure sets signOutError and does not navigate`() = runTest {
        coEvery { signOutUseCase.execute() } throws RuntimeException("disk full")

        viewModel.sendIntent(SettingsIntent.SignOut)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(withTimeoutOrNull(100) { viewModel.events.first() })
        val state = viewModel.uiState.value
        assertEquals(Res.string.sign_out_failed, state.signOutError)
        assertFalse(state.isSigningOut)
    }

    @Test
    fun `DismissSignOutError clears the error`() = runTest {
        coEvery { signOutUseCase.execute() } throws RuntimeException("disk full")
        viewModel.sendIntent(SettingsIntent.SignOut)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(SettingsIntent.DismissSignOutError)

        assertNull(viewModel.uiState.value.signOutError)
    }

    @Test
    fun `loadAccount stores the email of the current user when authenticated`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { authRepository.getCurrentUser() } returns
            Result.success(CurrentUser(email = "me@example.com", name = "Me", hasPassword = true))

        val signedInViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = signedInViewModel.uiState.value
        assertEquals("me@example.com", state.email)
        assertTrue(state.isSignedIn)
        assertTrue(state.hasPassword)
    }

    @Test
    fun `email stays null when not authenticated`() = runTest {
        assertNull(viewModel.uiState.value.email)
    }
}

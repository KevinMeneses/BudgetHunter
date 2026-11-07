package com.meneses.budgethunter.settings

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.sign_out_failed
import com.meneses.budgethunter.auth.application.SignOutUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.models.CurrentUser
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.application.SettingsIntent
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignOutConfirmationVisible)
    }

    @Test
    fun `ToggleSignOutConfirmation true warns when there are unsynced changes`() = runTest {
        coEvery { signOutUseCase.hasUnsyncedChanges() } returns true

        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignOutConfirmationVisible)
        assertTrue(viewModel.uiState.value.signOutLosesUnsyncedChanges)
    }

    @Test
    fun `ToggleSignOutConfirmation true does not warn when everything is synced`() = runTest {
        coEvery { signOutUseCase.hasUnsyncedChanges() } returns false

        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignOutConfirmationVisible)
        assertFalse(viewModel.uiState.value.signOutLosesUnsyncedChanges)
    }

    @Test
    fun `ToggleSignOutConfirmation true still shows the warning when unsynced changes cannot be checked`() = runTest {
        coEvery { signOutUseCase.hasUnsyncedChanges() } throws RuntimeException("db closed")

        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignOutConfirmationVisible)
        assertTrue(viewModel.uiState.value.signOutLosesUnsyncedChanges)
    }

    @Test
    fun `ToggleSignOutConfirmation false hides the confirmation`() = runTest {
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(false))

        assertFalse(viewModel.uiState.value.isSignOutConfirmationVisible)
    }

    @Test
    fun `cancelling before the unsynced check finishes keeps the confirmation closed`() = runTest {
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSignOutConfirmationVisible)
    }

    @Test
    fun `SignOut success runs the sign out and closes the confirmation`() = runTest {
        viewModel.sendIntent(SettingsIntent.ToggleSignOutConfirmation(true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(SettingsIntent.SignOut)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { signOutUseCase.execute() }
        val state = viewModel.uiState.value
        assertFalse(state.isSignOutConfirmationVisible)
        assertFalse(state.isSigningOut)
        assertNull(state.signOutError)
    }

    @Test
    fun `SignOut failure sets signOutError`() = runTest {
        coEvery { signOutUseCase.execute() } throws RuntimeException("keystore unavailable")

        viewModel.sendIntent(SettingsIntent.SignOut)
        testDispatcher.scheduler.advanceUntilIdle()

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

    @Test
    fun `turning on bank notifications without access explains it before the system settings`() = runTest {
        every { permissionsManager.hasNotificationListenerAccess() } returns false

        viewModel.sendIntent(SettingsIntent.ToggleBankNotificationReading(true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesManager.setBankNotificationReadingEnabled(true) }
        assertTrue(viewModel.uiState.value.isBankNotificationReadingEnabled)
        assertTrue(viewModel.uiState.value.isNotificationAccessDialogVisible)

        viewModel.sendIntent(SettingsIntent.OpenNotificationAccessSettings)

        verify { permissionsManager.openNotificationListenerSettings() }
        assertFalse(viewModel.uiState.value.isNotificationAccessDialogVisible)
    }

    @Test
    fun `turning on bank notifications with access granted asks nothing`() = runTest {
        every { permissionsManager.hasNotificationListenerAccess() } returns true

        viewModel.sendIntent(SettingsIntent.ToggleBankNotificationReading(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasNotificationListenerAccess)
        assertFalse(viewModel.uiState.value.isNotificationAccessDialogVisible)
    }

    @Test
    fun `coming back to the screen reads the notification access again`() = runTest {
        every { permissionsManager.hasNotificationListenerAccess() } returns true

        viewModel.sendIntent(SettingsIntent.RefreshPermissions)

        assertTrue(viewModel.uiState.value.hasNotificationListenerAccess)
    }
}

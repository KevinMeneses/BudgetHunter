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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
 * Unit tests for SettingsViewModel: the sign out flow, the account section and SMS reading.
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

    // ========== SMS reading and its permission ==========

    private var smsGranted = false
    private var smsRationale = false
    private val permissionCallback = slot<(Boolean) -> Unit>()

    private fun givenPermissions(granted: Boolean, rationale: Boolean = false) {
        smsGranted = granted
        smsRationale = rationale
        every { permissionsManager.hasSmsPermission() } answers { smsGranted }
        every { permissionsManager.hasNotificationPermission() } returns true
        every { permissionsManager.shouldShowSMSPermissionRationale() } answers { smsRationale }
        every { permissionsManager.requestSmsPermissions(capture(permissionCallback)) } returns Unit
    }

    /** What the system dialog answers. */
    private fun answerPermissionDialog(granted: Boolean, rationaleAfter: Boolean = !granted) {
        smsGranted = granted
        smsRationale = rationaleAfter
        permissionCallback.captured(granted)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `turning sms reading on with the permission granted saves it`() = runTest {
        givenPermissions(granted = true)

        viewModel.sendIntent(SettingsIntent.ToggleSmsReading(true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesManager.setSmsReadingEnabled(true) }
        verify(exactly = 0) { permissionsManager.requestSmsPermissions(any()) }
        assertTrue(viewModel.uiState.value.isSmsReadingActive)
    }

    @Test
    fun `turning sms reading on and granting the permission turns it on`() = runTest {
        givenPermissions(granted = false)

        viewModel.sendIntent(SettingsIntent.ToggleSmsReading(true))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { preferencesManager.setSmsReadingEnabled(true) }

        answerPermissionDialog(granted = true)

        coVerify { preferencesManager.setSmsReadingEnabled(true) }
        assertTrue(viewModel.uiState.value.isSmsReadingActive)
    }

    @Test
    fun `turning sms reading on and denying the permission keeps it off`() = runTest {
        givenPermissions(granted = false)

        viewModel.sendIntent(SettingsIntent.ToggleSmsReading(true))
        testDispatcher.scheduler.advanceUntilIdle()
        answerPermissionDialog(granted = false)

        coVerify(exactly = 0) { preferencesManager.setSmsReadingEnabled(true) }
        val state = viewModel.uiState.value
        assertFalse(state.isSmsReadingActive)
        assertFalse(state.isSmsPermissionMissing)
        assertFalse(state.isManualPermissionDialogVisible)
    }

    @Test
    fun `a permission denied for good leads to the app settings and turns on on return`() = runTest {
        givenPermissions(granted = false)

        viewModel.sendIntent(SettingsIntent.ToggleSmsReading(true))
        testDispatcher.scheduler.advanceUntilIdle()
        // The system does not show its dialog any more: denied, and no rationale either
        answerPermissionDialog(granted = false, rationaleAfter = false)
        assertTrue(viewModel.uiState.value.isManualPermissionDialogVisible)

        viewModel.sendIntent(SettingsIntent.OpenAppSettings)
        smsGranted = true
        viewModel.sendIntent(SettingsIntent.RefreshPermissions)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesManager.setSmsReadingEnabled(true) }
        assertTrue(viewModel.uiState.value.isSmsReadingActive)
    }

    @Test
    fun `cancelling the app settings dialog keeps sms reading off`() = runTest {
        givenPermissions(granted = false, rationale = true)

        viewModel.sendIntent(SettingsIntent.ToggleSmsReading(true))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendIntent(SettingsIntent.HideManualPermissionDialog)
        smsGranted = true
        viewModel.sendIntent(SettingsIntent.RefreshPermissions)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { preferencesManager.setSmsReadingEnabled(true) }
    }

    @Test
    fun `a restored preference without the permission shows the warning`() = runTest {
        givenPermissions(granted = false)
        coEvery { preferencesManager.isSmsReadingEnabled() } returns true

        val restoredViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = restoredViewModel.uiState.value
        assertTrue(state.isSmsPermissionMissing)
        assertFalse(state.isSmsReadingActive)
    }

    @Test
    fun `denying from the warning keeps the restored preference`() = runTest {
        givenPermissions(granted = false)
        coEvery { preferencesManager.isSmsReadingEnabled() } returns true
        val restoredViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        restoredViewModel.sendIntent(SettingsIntent.GrantSmsPermission)
        answerPermissionDialog(granted = false)

        coVerify(exactly = 0) { preferencesManager.setSmsReadingEnabled(false) }
        assertTrue(restoredViewModel.uiState.value.isSmsPermissionMissing)
    }

    @Test
    fun `granting from the warning turns the switch on`() = runTest {
        givenPermissions(granted = false)
        coEvery { preferencesManager.isSmsReadingEnabled() } returns true
        val restoredViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        restoredViewModel.sendIntent(SettingsIntent.GrantSmsPermission)
        answerPermissionDialog(granted = true)

        val state = restoredViewModel.uiState.value
        assertTrue(state.isSmsReadingActive)
        assertFalse(state.isSmsPermissionMissing)
    }

    @Test
    fun `a permission granted in the system settings is picked up on resume`() = runTest {
        givenPermissions(granted = false)
        coEvery { preferencesManager.isSmsReadingEnabled() } returns true
        val restoredViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        smsGranted = true
        restoredViewModel.sendIntent(SettingsIntent.RefreshPermissions)

        assertTrue(restoredViewModel.uiState.value.isSmsReadingActive)
    }

    @Test
    fun `turning sms reading off saves it`() = runTest {
        givenPermissions(granted = true)

        viewModel.sendIntent(SettingsIntent.ToggleSmsReading(false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesManager.setSmsReadingEnabled(false) }
        coVerify { syncUserPreferences.push() }
    }
}

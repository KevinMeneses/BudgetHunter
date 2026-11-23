package com.meneses.budgethunter.splash

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.AppUpdateResult
import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.splash.application.SplashIntent
import io.mockk.coEvery
import io.mockk.coVerify
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

/**
 * Unit tests for SplashScreenViewModel event emissions.
 *
 * Verifies that the ViewModel emits the correct navigation event based on
 * the user's authentication state after checking for app updates.
 *
 * The `AppUpdateManager.checkForUpdates` is callback-based; in these tests it is
 * stubbed to immediately invoke `onResult` with `NoUpdateAvailable`, which causes
 * `setNavigateState()` to be called and the appropriate SplashEvent to be emitted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)

    // System under test
    private lateinit var viewModel: SplashScreenViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Default: offline mode is disabled
        coEvery { preferencesManager.isOfflineModeEnabled() } returns false

        // Stub checkForUpdates to immediately report no update available so navigation proceeds
        every { appUpdateManager.checkForUpdates(any()) } answers {
            val callback = firstArg<(AppUpdateResult) -> Unit>()
            callback(AppUpdateResult.NoUpdateAvailable)
        }

        viewModel = SplashScreenViewModel(
            appUpdateManager = appUpdateManager,
            authRepository = authRepository,
            preferencesManager = preferencesManager
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== verifyUpdate when authenticated Tests ==========

    @Test
    fun `verifyUpdate when authenticated emits NavigateToBudgetList event`() = runTest {
        // Given - user is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        // When
        viewModel.sendIntent(SplashIntent.VerifyUpdate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - navigation to budget list is emitted
        val event = viewModel.events.first()
        assertIs<SplashEvent.NavigateToBudgetList>(event)
    }

    // ========== verifyUpdate when not authenticated Tests ==========

    @Test
    fun `verifyUpdate when not authenticated emits NavigateToSignIn event`() = runTest {
        // Given - user is not authenticated and offline mode is off
        coEvery { authRepository.isAuthenticated() } returns false
        coEvery { preferencesManager.isOfflineModeEnabled() } returns false

        // When
        viewModel.sendIntent(SplashIntent.VerifyUpdate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - navigation to sign-in screen is emitted
        val event = viewModel.events.first()
        assertIs<SplashEvent.NavigateToSignIn>(event)
    }

    // ========== offline mode left on with a session Tests ==========

    @Test
    fun `verifyUpdate with a session clears an offline mode left on`() = runTest {
        // Given - an account that signed in from offline mode before the flag was cleared on sign in
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { preferencesManager.isOfflineModeEnabled() } returns true

        // When
        viewModel.sendIntent(SplashIntent.VerifyUpdate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - the flag is cleared before navigating, so the privacy check sent with it runs
        coVerify(exactly = 1) { preferencesManager.setOfflineModeEnabled(false) }
        assertIs<SplashEvent.NavigateToBudgetList>(viewModel.events.first())
    }

    @Test
    fun `verifyUpdate keeps offline mode without a session`() = runTest {
        // Given - plain offline mode
        coEvery { authRepository.isAuthenticated() } returns false
        coEvery { preferencesManager.isOfflineModeEnabled() } returns true

        // When
        viewModel.sendIntent(SplashIntent.VerifyUpdate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { preferencesManager.setOfflineModeEnabled(any()) }
        assertIs<SplashEvent.NavigateToBudgetList>(viewModel.events.first())
    }
}

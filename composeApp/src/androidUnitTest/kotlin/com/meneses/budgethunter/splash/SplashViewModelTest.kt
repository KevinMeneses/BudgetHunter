package com.meneses.budgethunter.splash

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.AppUpdateResult
import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.splash.application.SplashIntent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    // ========== Play does not answer (time limit) Tests ==========

    private val limit = SplashScreenViewModel.UPDATE_CHECK_TIMEOUT_MS
    private var captured: ((AppUpdateResult) -> Unit)? = null

    /** Play never answers on its own: the test delivers answers through [captured]. */
    private fun TestScope.startSilentCheck(authenticated: Boolean): MutableList<SplashEvent> {
        captured = null
        every { appUpdateManager.checkForUpdates(any()) } answers { captured = firstArg() }
        coEvery { authRepository.isAuthenticated() } returns authenticated
        val events = mutableListOf<SplashEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        viewModel.sendIntent(SplashIntent.VerifyUpdate)
        testScheduler.runCurrent()
        return events
    }

    private fun TestScope.pastLimit() {
        testScheduler.advanceTimeBy(limit + 200 + 1)
        testScheduler.runCurrent()
    }

    @Test
    fun `no answer from Play navigates to budget list once after the limit`() = runTest {
        // Given - authenticated and Play never answers
        val events = startSilentCheck(authenticated = true)

        // When - just before the limit
        testScheduler.advanceTimeBy(limit - 1)
        testScheduler.runCurrent()

        // Then - nothing yet
        assertTrue(events.isEmpty())

        // When - the limit and the delay pass
        pastLimit()

        // Then
        assertEquals(1, events.size)
        assertIs<SplashEvent.NavigateToBudgetList>(events.single())
    }

    @Test
    fun `no answer from Play navigates to sign in when not authenticated`() = runTest {
        // Given
        val events = startSilentCheck(authenticated = false)

        // When
        pastLimit()

        // Then
        assertIs<SplashEvent.NavigateToSignIn>(events.single())
    }

    @Test
    fun `late NoUpdateAvailable after the limit does not navigate again`() = runTest {
        // Given - the limit already navigated
        val events = startSilentCheck(authenticated = true)
        pastLimit()

        // When - Play finally answers
        captured!!.invoke(AppUpdateResult.NoUpdateAvailable)
        pastLimit()

        // Then
        assertEquals(1, events.size)
    }

    @Test
    fun `late UpdateAvailable after the limit is ignored`() = runTest {
        // Given
        val events = startSilentCheck(authenticated = true)
        pastLimit()
        var started = false

        // When
        captured!!.invoke(AppUpdateResult.UpdateAvailable(startUpdate = { started = true }))
        pastLimit()

        // Then
        assertFalse(started, "startUpdate must not run after the limit")
        assertEquals(1, events.size)
    }

    @Test
    fun `late UpdateInProgress after the limit leaves updatingApp false`() = runTest {
        // Given
        startSilentCheck(authenticated = true)
        pastLimit()

        // When
        captured!!.invoke(AppUpdateResult.UpdateInProgress)
        testScheduler.runCurrent()

        // Then
        assertFalse(viewModel.uiState.value.updatingApp)
    }

    @Test
    fun `UpdateInProgress then silence navigates once after another limit`() = runTest {
        // Given - Play answers at 1 s that an update is running, and then nothing
        // (no install listener is registered in this process, so it may never answer again)
        val events = startSilentCheck(authenticated = true)
        testScheduler.advanceTimeBy(1_000)
        captured!!.invoke(AppUpdateResult.UpdateInProgress)
        testScheduler.runCurrent()

        // When - just before the limit counted from that answer
        testScheduler.advanceTimeBy(limit - 1)
        testScheduler.runCurrent()

        // Then - still waiting, the update is shown
        assertTrue(events.isEmpty())
        assertTrue(viewModel.uiState.value.updatingApp)

        // When - past it
        pastLimit()

        // Then - the app moved on without any further answer
        assertEquals(1, events.size)

        // When - a late answer
        captured!!.invoke(AppUpdateResult.NoUpdateAvailable)
        pastLimit()

        // Then - one navigation only
        assertEquals(1, events.size)
    }

    @Test
    fun `UpdateInProgress that finishes in time navigates once`() = runTest {
        // Given
        val events = startSilentCheck(authenticated = true)
        captured!!.invoke(AppUpdateResult.UpdateInProgress)
        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()

        // When - the update finishes before the limit
        captured!!.invoke(AppUpdateResult.NoUpdateAvailable)
        testScheduler.advanceTimeBy(limit * 3)
        testScheduler.runCurrent()

        // Then
        assertEquals(1, events.size)
    }

    @Test
    fun `UpdateAvailable with no flow behind it navigates after the limit`() = runTest {
        // Given - Play reports an update; starting it shows nothing and never answers again
        // (no launcher delegate on Android; the App Store on iOS)
        val events = startSilentCheck(authenticated = true)
        var started = false
        captured!!.invoke(AppUpdateResult.UpdateAvailable(startUpdate = { started = true }))
        testScheduler.runCurrent()

        // When
        pastLimit()

        // Then - the update was offered, and the app moved on anyway
        assertTrue(started)
        assertIs<SplashEvent.NavigateToBudgetList>(events.single())
    }

    @Test
    fun `NoUpdateAvailable delivered twice in time navigates once`() = runTest {
        // Given
        val events = startSilentCheck(authenticated = true)

        // When
        captured!!.invoke(AppUpdateResult.NoUpdateAvailable)
        captured!!.invoke(AppUpdateResult.NoUpdateAvailable)
        pastLimit()

        // Then
        assertEquals(1, events.size)
    }
}

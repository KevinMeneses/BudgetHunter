package com.meneses.budgethunter.splash

import com.meneses.budgethunter.commons.platform.AppUpdateManager
import com.meneses.budgethunter.commons.platform.AppUpdateResult
import com.meneses.budgethunter.splash.application.SplashEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplashScreenViewModelTest {

    private class FakeAppUpdateManager(
        private val updateResult: AppUpdateResult
    ) : AppUpdateManager {
        var checkForUpdatesCalled = false
        var startUpdateCalled = false

        override fun checkForUpdates(callback: (AppUpdateResult) -> Unit) {
            checkForUpdatesCalled = true
            callback(updateResult)
        }
    }

    private class FakeAppUpdateResult(
        private val onStart: () -> Unit = {}
    ) : AppUpdateResult.UpdateAvailable {
        override fun startUpdate() {
            onStart()
        }
    }

    @Test
    fun `initial state has navigate and updatingApp false`() = runTest {
        val fakeManager = FakeAppUpdateManager(AppUpdateResult.NoUpdateAvailable)
        val viewModel = SplashScreenViewModel(fakeManager)

        val state = viewModel.uiState.value
        assertFalse(state.navigate)
        assertFalse(state.updatingApp)
    }

    @Test
    fun `verifyUpdate calls checkForUpdates on app update manager`() = runTest {
        val fakeManager = FakeAppUpdateManager(AppUpdateResult.NoUpdateAvailable)
        val viewModel = SplashScreenViewModel(fakeManager)

        viewModel.sendEvent(SplashEvent.VerifyUpdate)

        assertTrue(fakeManager.checkForUpdatesCalled)
    }

    @Test
    fun `verifyUpdate sets navigate to true when no update available`() = runTest {
        val fakeManager = FakeAppUpdateManager(AppUpdateResult.NoUpdateAvailable)
        val viewModel = SplashScreenViewModel(fakeManager)

        viewModel.sendEvent(SplashEvent.VerifyUpdate)

        val state = viewModel.uiState.value
        assertTrue(state.navigate)
        assertFalse(state.updatingApp)
    }

    @Test
    fun `verifyUpdate sets updatingApp to true when update in progress`() = runTest {
        val fakeManager = FakeAppUpdateManager(AppUpdateResult.UpdateInProgress)
        val viewModel = SplashScreenViewModel(fakeManager)

        viewModel.sendEvent(SplashEvent.VerifyUpdate)

        val state = viewModel.uiState.value
        assertFalse(state.navigate)
        assertTrue(state.updatingApp)
    }

    @Test
    fun `verifyUpdate calls startUpdate when update available`() = runTest {
        var startUpdateCalled = false
        val updateResult = FakeAppUpdateResult { startUpdateCalled = true }
        val fakeManager = FakeAppUpdateManager(updateResult)
        val viewModel = SplashScreenViewModel(fakeManager)

        viewModel.sendEvent(SplashEvent.VerifyUpdate)

        assertTrue(startUpdateCalled)
        val state = viewModel.uiState.value
        assertFalse(state.navigate)
        assertFalse(state.updatingApp)
    }

    @Test
    fun `verifyUpdate sets navigate to true when update failed`() = runTest {
        val fakeManager = FakeAppUpdateManager(AppUpdateResult.UpdateFailed)
        val viewModel = SplashScreenViewModel(fakeManager)

        viewModel.sendEvent(SplashEvent.VerifyUpdate)

        val state = viewModel.uiState.value
        assertTrue(state.navigate)
        assertFalse(state.updatingApp)
    }

    @Test
    fun `verifyUpdate can be called multiple times`() = runTest {
        val fakeManager = FakeAppUpdateManager(AppUpdateResult.NoUpdateAvailable)
        val viewModel = SplashScreenViewModel(fakeManager)

        viewModel.sendEvent(SplashEvent.VerifyUpdate)
        viewModel.sendEvent(SplashEvent.VerifyUpdate)

        assertTrue(fakeManager.checkForUpdatesCalled)
    }

    @Test
    fun `state changes are reflected in uiState flow`() = runTest {
        val fakeManager = FakeAppUpdateManager(AppUpdateResult.UpdateInProgress)
        val viewModel = SplashScreenViewModel(fakeManager)

        val initialState = viewModel.uiState.value
        assertFalse(initialState.navigate)
        assertFalse(initialState.updatingApp)

        viewModel.sendEvent(SplashEvent.VerifyUpdate)

        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.updatingApp)
    }
}

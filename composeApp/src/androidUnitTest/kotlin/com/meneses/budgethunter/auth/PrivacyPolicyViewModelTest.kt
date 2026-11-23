package com.meneses.budgethunter.auth

import com.meneses.budgethunter.auth.application.CheckPrivacyPolicyUseCase
import com.meneses.budgethunter.auth.application.PrivacyPolicyIntent
import com.meneses.budgethunter.auth.application.SignOutUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyPolicyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val checkPrivacyPolicy = mockk<CheckPrivacyPolicyUseCase>()
    private val authRepository = mockk<AuthRepository>()
    private val signOutUseCase = mockk<SignOutUseCase>(relaxed = true)
    private lateinit var viewModel: PrivacyPolicyViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { signOutUseCase.hasUnsyncedChanges() } returns false
        viewModel = PrivacyPolicyViewModel(
            checkPrivacyPolicy = checkPrivacyPolicy,
            authRepository = authRepository,
            signOutUseCase = signOutUseCase,
            applicationScope = CoroutineScope(testDispatcher),
            logger = NoOpLogger()
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun send(intent: PrivacyPolicyIntent) {
        viewModel.sendIntent(intent)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun showDialogFor(version: String) {
        coEvery { checkPrivacyPolicy.pendingVersion() } returns version
        send(PrivacyPolicyIntent.Check)
    }

    @Test
    fun `the dialog shows when the versions differ`() = runTest {
        showDialogFor("2026-11-23")

        assertTrue(viewModel.uiState.value.isDialogVisible)
        assertEquals("2026-11-23", viewModel.uiState.value.pendingVersion)
    }

    @Test
    fun `the dialog stays hidden when the versions match, or the check could not tell`() = runTest {
        coEvery { checkPrivacyPolicy.pendingVersion() } returns null

        send(PrivacyPolicyIntent.Check)

        assertFalse(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun `accept sends the server's version and closes the dialog`() = runTest {
        showDialogFor("2026-11-23")
        coEvery { authRepository.acceptPrivacyPolicy(any()) } returns Result.success(Unit)

        send(PrivacyPolicyIntent.Accept)

        coVerify(exactly = 1) { authRepository.acceptPrivacyPolicy("2026-11-23") }
        assertFalse(viewModel.uiState.value.isDialogVisible)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `a failed accept does not block the app and the next check asks again`() = runTest {
        showDialogFor("2026-11-23")
        coEvery { authRepository.acceptPrivacyPolicy(any()) } returns Result.failure(java.io.IOException("offline"))

        send(PrivacyPolicyIntent.Accept)

        assertFalse(viewModel.uiState.value.isDialogVisible)
        send(PrivacyPolicyIntent.Check)
        assertTrue(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun `sign out calls SignOutUseCase and closes the dialog`() = runTest {
        showDialogFor("2026-11-23")

        send(PrivacyPolicyIntent.SignOut)

        coVerify(exactly = 1) { signOutUseCase.execute() }
        coVerify(exactly = 0) { authRepository.acceptPrivacyPolicy(any()) }
        assertFalse(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun `sign out with unsynced changes warns first, and only the confirmation signs out`() = runTest {
        showDialogFor("2026-11-23")
        coEvery { signOutUseCase.hasUnsyncedChanges() } returns true

        send(PrivacyPolicyIntent.SignOut)
        assertTrue(viewModel.uiState.value.isSignOutConfirmationVisible)
        coVerify(exactly = 0) { signOutUseCase.execute() }

        send(PrivacyPolicyIntent.CancelSignOut)
        assertFalse(viewModel.uiState.value.isSignOutConfirmationVisible)
        assertTrue(viewModel.uiState.value.isDialogVisible)

        send(PrivacyPolicyIntent.SignOut)
        send(PrivacyPolicyIntent.ConfirmSignOut)
        coVerify(exactly = 1) { signOutUseCase.execute() }
        assertFalse(viewModel.uiState.value.isDialogVisible)
    }

    @Test
    fun `a failed sign out does not block the app`() = runTest {
        showDialogFor("2026-11-23")
        coEvery { signOutUseCase.execute() } throws IllegalStateException("keystore")

        send(PrivacyPolicyIntent.SignOut)

        assertFalse(viewModel.uiState.value.isDialogVisible)
        assertNull(viewModel.uiState.value.pendingVersion)
    }

    @Test
    fun `a check while the dialog is open asks the server nothing more`() = runTest {
        showDialogFor("2026-11-23")

        send(PrivacyPolicyIntent.Check)

        coVerify(exactly = 1) { checkPrivacyPolicy.pendingVersion() }
    }
}

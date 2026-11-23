package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.models.CurrentUser
import com.meneses.budgethunter.commons.data.network.models.PrivacyPolicyStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheckPrivacyPolicyUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val useCase = CheckPrivacyPolicyUseCase(authRepository, preferencesManager)

    @BeforeTest
    fun setup() {
        coEvery { preferencesManager.isOfflineModeEnabled() } returns false
        coEvery { authRepository.isAuthenticated() } returns true
    }

    private fun serverSays(status: PrivacyPolicyStatus?) {
        coEvery { authRepository.getCurrentUser() } returns
            Result.success(CurrentUser(email = "a@b.com", name = "A", privacyPolicy = status))
    }

    @Test
    fun `a different accepted version asks for the server's current one`() = runTest {
        serverSays(PrivacyPolicyStatus(currentVersion = "2027-01-10", acceptedVersion = "2026-11-23"))

        assertEquals("2027-01-10", useCase.pendingVersion())
    }

    @Test
    fun `an account that never accepted is asked`() = runTest {
        serverSays(PrivacyPolicyStatus(currentVersion = "2026-11-23", acceptedVersion = null))

        assertEquals("2026-11-23", useCase.pendingVersion())
    }

    @Test
    fun `the same version asks nothing`() = runTest {
        serverSays(PrivacyPolicyStatus(currentVersion = "2026-11-23", acceptedVersion = "2026-11-23"))

        assertNull(useCase.pendingVersion())
    }

    @Test
    fun `a server that does not report the policy asks nothing`() = runTest {
        serverSays(null)

        assertNull(useCase.pendingVersion())
    }

    @Test
    fun `no connection asks nothing`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns Result.failure(java.io.IOException("offline"))

        assertNull(useCase.pendingVersion())
    }

    @Test
    fun `offline mode asks nothing and never calls the server`() = runTest {
        coEvery { preferencesManager.isOfflineModeEnabled() } returns true

        assertNull(useCase.pendingVersion())
        coVerify(exactly = 0) { authRepository.getCurrentUser() }
    }

    @Test
    fun `no session asks nothing and never calls the server`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns false

        assertNull(useCase.pendingVersion())
        coVerify(exactly = 0) { authRepository.getCurrentUser() }
    }

    @Test
    fun `signed out while the answer was on its way asks nothing`() = runTest {
        coEvery { authRepository.isAuthenticated() } returnsMany listOf(true, false)
        serverSays(PrivacyPolicyStatus(currentVersion = "2026-11-23", acceptedVersion = null))

        assertNull(useCase.pendingVersion())
    }
}

package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.network.models.AuthResponse
import com.meneses.budgethunter.commons.platform.GoogleSignInManager
import com.meneses.budgethunter.commons.platform.GoogleSignInResult
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignInWithGoogleUseCaseTest {

    private val googleSignInManager = mockk<GoogleSignInManager>()
    private val authRepository = mockk<AuthRepository>()
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val budgetEntrySyncManager = mockk<BudgetEntrySyncManager>(relaxed = true)
    private val syncUserPreferences = mockk<SyncUserPreferencesUseCase>(relaxed = true)
    private val prepareDataForAccount = mockk<PrepareDataForAccountUseCase>(relaxed = true)

    // Unconfined so the launched sync runs eagerly and its ordering can be verified
    private val useCase = SignInWithGoogleUseCase(
        googleSignInManager = googleSignInManager,
        authRepository = authRepository,
        budgetRepository = budgetRepository,
        budgetEntrySyncManager = budgetEntrySyncManager,
        syncUserPreferences = syncUserPreferences,
        prepareDataForAccount = prepareDataForAccount,
        applicationScope = CoroutineScope(Dispatchers.Unconfined)
    )

    private fun pickerReturns(result: GoogleSignInResult) {
        every { googleSignInManager.signIn(any()) } answers {
            firstArg<(GoogleSignInResult) -> Unit>().invoke(result)
        }
    }

    @Test
    fun `success prepares data for the server email before syncing`() = runTest {
        pickerReturns(GoogleSignInResult.Success("id-token"))
        coEvery { authRepository.signInWithGoogle("id-token") } returns
            Result.success(AuthResponse("auth", "refresh", "google@example.com", "Google User"))

        val outcome = useCase.execute()

        assertEquals(GoogleAuthOutcome.Success("google@example.com"), outcome)
        coVerifyOrder {
            prepareDataForAccount.execute("google@example.com")
            budgetRepository.sync()
            budgetEntrySyncManager.syncAllBudgetsEntries()
            syncUserPreferences.pull()
        }
    }

    @Test
    fun `backend rejection does not prepare or sync`() = runTest {
        pickerReturns(GoogleSignInResult.Success("id-token"))
        coEvery { authRepository.signInWithGoogle("id-token") } returns Result.failure(Exception("rejected"))

        val outcome = useCase.execute()

        assertEquals(GoogleAuthOutcome.Failed, outcome)
        coVerify(exactly = 0) { prepareDataForAccount.execute(any()) }
        coVerify(exactly = 0) { budgetRepository.sync() }
    }

    @Test
    fun `cancelled picker does not prepare data`() = runTest {
        pickerReturns(GoogleSignInResult.Cancelled)

        val outcome = useCase.execute()

        assertEquals(GoogleAuthOutcome.Cancelled, outcome)
        coVerify(exactly = 0) { prepareDataForAccount.execute(any()) }
    }

    @Test
    fun `platform failure does not prepare data`() = runTest {
        pickerReturns(GoogleSignInResult.Failure("boom"))

        val outcome = useCase.execute()

        assertEquals(GoogleAuthOutcome.Failed, outcome)
        coVerify(exactly = 0) { prepareDataForAccount.execute(any()) }
    }
}

package com.meneses.budgethunter.collaborator.data

import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.collaborator.data.network.CollaboratorApiService
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollaboratorRepositoryTest {

    private val apiService = mockk<CollaboratorApiService>()
    private val budgetLocalDataSource = mockk<BudgetLocalDataSource>(relaxed = true)
    private val repository = CollaboratorRepository(apiService, budgetLocalDataSource, Dispatchers.Unconfined)

    @Test
    fun `getCollaborators stores the list size as the collaborator count`() = runTest {
        val users = listOf(UserInfo("a@a.com", "A"), UserInfo("b@b.com", "B"), UserInfo("c@c.com", "C"))
        coEvery { apiService.getCollaborators(42L) } returns Result.success(users)

        val result = repository.getCollaborators(42L)

        assertEquals(users, result.getOrNull())
        coVerify(exactly = 1) { budgetLocalDataSource.updateCollaboratorCount(42L, 3) }
    }

    @Test
    fun `getCollaborators does not store a count when the request fails`() = runTest {
        coEvery { apiService.getCollaborators(42L) } returns Result.failure(RuntimeException("offline"))

        val result = repository.getCollaborators(42L)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { budgetLocalDataSource.updateCollaboratorCount(any(), any()) }
    }
}

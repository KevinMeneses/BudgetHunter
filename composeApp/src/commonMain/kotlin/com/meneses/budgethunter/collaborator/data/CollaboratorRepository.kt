package com.meneses.budgethunter.collaborator.data

import com.meneses.budgethunter.collaborator.data.network.CollaboratorApiService
import com.meneses.budgethunter.commons.data.network.models.AddCollaboratorRequest
import com.meneses.budgethunter.commons.data.network.models.CollaboratorResponse
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Repository layer for collaborator operations.
 * Coordinates between the API service and manages collaborator-related data operations.
 */
class CollaboratorRepository(
    private val collaboratorApiService: CollaboratorApiService,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Adds a collaborator to a budget by their email address.
     *
     * @param budgetServerId Server-side budget ID
     * @param email Email address of the user to add as collaborator
     * @return Result containing the collaborator response or error
     */
    suspend fun addCollaborator(
        budgetServerId: Long,
        email: String
    ): Result<CollaboratorResponse> = withContext(ioDispatcher) {
        val request = AddCollaboratorRequest(
            budgetId = budgetServerId,
            email = email
        )
        collaboratorApiService.addCollaborator(budgetServerId, request)
    }

    /**
     * Fetches all collaborators for a specific budget.
     *
     * @param budgetServerId Server-side budget ID
     * @return Result containing list of collaborators (UserInfo) or error
     */
    suspend fun getCollaborators(budgetServerId: Long): Result<List<UserInfo>> =
        withContext(ioDispatcher) {
            collaboratorApiService.getCollaborators(budgetServerId)
        }
}

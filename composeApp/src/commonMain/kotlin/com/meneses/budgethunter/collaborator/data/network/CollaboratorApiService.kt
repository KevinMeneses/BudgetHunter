package com.meneses.budgethunter.collaborator.data.network

import com.meneses.budgethunter.commons.data.network.models.AddCollaboratorRequest
import com.meneses.budgethunter.commons.data.network.models.CollaboratorResponse
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * API service for collaborator-related network operations.
 * Handles communication with the backend for collaborator management using RESTful conventions.
 */
class CollaboratorApiService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Adds a collaborator to a budget.
     *
     * RESTful endpoint: POST /api/budgets/{budgetId}/collaborators
     *
     * @param budgetId Server-side budget ID (passed in URL path)
     * @param request Collaborator addition request with budgetId and email
     * @return Result containing the collaborator response or error
     */
    suspend fun addCollaborator(
        budgetId: Long,
        request: AddCollaboratorRequest
    ): Result<CollaboratorResponse> = withContext(ioDispatcher) {
        try {
            println("CollaboratorApiService: Adding collaborator to budget $budgetId with request: $request")
            val response: CollaboratorResponse = httpClient.post("/api/budgets/$budgetId/collaborators") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            println("CollaboratorApiService: Successfully added collaborator: $response")
            Result.success(response)
        } catch (e: Exception) {
            println("CollaboratorApiService: Error adding collaborator - ${e.message}")
            println("CollaboratorApiService: Error type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(Exception("Failed to add collaborator to budget: ${e.message}", e))
        }
    }

    /**
     * Fetches all collaborators for a specific budget from the server.
     *
     * RESTful endpoint: GET /api/budgets/{budgetId}/collaborators
     *
     * @param budgetId Server-side budget ID (passed in URL path)
     * @return Result containing list of collaborators (UserInfo) or error
     */
    suspend fun getCollaborators(budgetId: Long): Result<List<UserInfo>> =
        withContext(ioDispatcher) {
            try {
                println("CollaboratorApiService: Fetching collaborators for budget $budgetId")
                val response = httpClient.get("/api/budgets/$budgetId/collaborators")
                val collaborators = response.body<List<UserInfo>>()
                println("CollaboratorApiService: Successfully fetched ${collaborators.size} collaborators")
                Result.success(collaborators)
            } catch (e: Exception) {
                println("CollaboratorApiService: Error fetching collaborators - ${e.message}")
                e.printStackTrace()
                Result.failure(Exception("Failed to fetch collaborators: ${e.message}", e))
            }
        }
}

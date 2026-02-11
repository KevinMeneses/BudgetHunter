package com.meneses.budgethunter.collaborator.data.network

import com.meneses.budgethunter.commons.data.network.models.AddCollaboratorRequest
import com.meneses.budgethunter.commons.data.network.models.CollaboratorResponse
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import com.meneses.budgethunter.commons.data.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
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
        println("CollaboratorApiService: Adding collaborator to budget $budgetId with request: $request")
        safeApiCall {
            httpClient.post("/api/budgets/$budgetId/collaborators") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<CollaboratorResponse>().also {
                println("CollaboratorApiService: Successfully added collaborator: $it")
            }
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
            println("CollaboratorApiService: Fetching collaborators for budget $budgetId")
            safeApiCall {
                httpClient.get("/api/budgets/$budgetId/collaborators")
                    .body<List<UserInfo>>()
                    .also { println("CollaboratorApiService: Successfully fetched ${it.size} collaborators") }
            }
        }

    /**
     * Removes a collaborator from a budget.
     *
     * RESTful endpoint: DELETE /api/budgets/{budgetId}/collaborators/{email}
     *
     * @param budgetId Server-side budget ID (passed in URL path)
     * @param email Email address of the collaborator to remove (URL-encoded in path)
     * @return Result containing success or error
     */
    suspend fun removeCollaborator(
        budgetId: Long,
        email: String
    ): Result<Unit> = withContext(ioDispatcher) {
        println("CollaboratorApiService: Removing collaborator $email from budget $budgetId")
        safeApiCall {
            val encodedEmail = email.encodeURLPath()
            httpClient.delete("/api/budgets/$budgetId/collaborators/$encodedEmail")
            println("CollaboratorApiService: Successfully removed collaborator: $email")
        }
    }
}

package com.meneses.budgethunter.budgetList.data.network

import com.meneses.budgethunter.commons.data.network.models.BudgetResponse
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetRequest
import com.meneses.budgethunter.commons.data.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * API service for budget-related network operations.
 * Handles communication with the backend for budget CRUD operations.
 */
class BudgetApiService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Creates a new budget on the server.
     *
     * @param request Budget creation request with name and amount
     * @return Result containing the created budget response or error
     */
    suspend fun createBudget(request: CreateBudgetRequest): Result<BudgetResponse> =
        withContext(ioDispatcher) {
            safeApiCall {
                httpClient.post("/api/budgets") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<BudgetResponse>()
            }
        }

    /**
     * Fetches all budgets for the authenticated user from the server.
     *
     * @return Result containing list of budget responses or error
     */
    suspend fun getBudgets(): Result<List<BudgetResponse>> =
        withContext(ioDispatcher) {
            safeApiCall {
                httpClient.get("/api/budgets")
                    .body<List<BudgetResponse>>()
            }
        }

    /**
     * Deletes a budget from the server.
     * Deletes the budget and all associated entries and collaborator relationships.
     * Returns 204 No Content on success.
     *
     * @param budgetId Server-side budget ID
     * @return Result containing Unit on success or error
     */
    suspend fun deleteBudget(budgetId: Long): Result<Unit> =
        withContext(ioDispatcher) {
            safeApiCall {
                httpClient.delete("/api/budgets/$budgetId").body()
            }
        }
}

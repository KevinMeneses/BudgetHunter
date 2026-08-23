package com.meneses.budgethunter.budgetEntry.data.network

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryResponse
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetEntryRequest
import com.meneses.budgethunter.commons.data.network.models.UpdateBudgetEntryRequest
import com.meneses.budgethunter.commons.data.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * API service for budget entry-related network operations.
 * Handles communication with the backend for budget entry CRUD operations using RESTful conventions.
 */
class BudgetEntryApiService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Creates a new budget entry on the server.
     *
     * RESTful endpoint: POST /api/budgets/{budgetId}/entries
     *
     * @param budgetId Server-side budget ID (passed in URL path)
     * @param request Entry creation request with amount, description, category, and type
     * @return Result containing the created budget entry response or error
     */
    suspend fun createEntry(
        budgetId: Long,
        request: CreateBudgetEntryRequest
    ): Result<BudgetEntryResponse> = withContext(ioDispatcher) {
        safeApiCall {
            httpClient.post(ApiEndpoints.budgetEntries(budgetId)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<BudgetEntryResponse>()
        }
    }

    /**
     * Updates an existing budget entry on the server.
     *
     * RESTful endpoint: PUT /api/budgets/{budgetId}/entries/{entryId}
     *
     * @param budgetId Server-side budget ID (passed in URL path)
     * @param entryId Server-side entry ID (passed in URL path)
     * @param request Entry update request with amount, description, category, and type
     * @return Result containing the updated budget entry response or error
     */
    suspend fun updateEntry(
        budgetId: Long,
        entryId: Long,
        request: UpdateBudgetEntryRequest
    ): Result<BudgetEntryResponse> = withContext(ioDispatcher) {
        safeApiCall {
            httpClient.put(ApiEndpoints.budgetEntry(budgetId, entryId)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<BudgetEntryResponse>()
        }
    }

    /**
     * Fetches all budget entries for a specific budget from the server.
     *
     * RESTful endpoint: GET /api/budgets/{budgetId}/entries
     *
     * @param budgetId Server-side budget ID (passed in URL path)
     * @return Result containing list of budget entry responses or error
     */
    suspend fun getEntries(budgetId: Long): Result<List<BudgetEntryResponse>> =
        withContext(ioDispatcher) {
            safeApiCall {
                httpClient.get(ApiEndpoints.budgetEntries(budgetId))
                    .body<List<BudgetEntryResponse>>()
            }
        }

    /**
     * Deletes a budget entry on the server.
     *
     * RESTful endpoint: DELETE /api/budgets/{budgetId}/entries/{entryId}
     *
     * @param budgetId Server-side budget ID (passed in URL path)
     * @param entryId Server-side entry ID (passed in URL path)
     * @return Result containing Unit on success or error
     */
    suspend fun deleteEntry(
        budgetId: Long,
        entryId: Long
    ): Result<Unit> = withContext(ioDispatcher) {
        safeApiCall {
            httpClient.delete(ApiEndpoints.budgetEntry(budgetId, entryId)).body<Unit>()
        }
    }
}

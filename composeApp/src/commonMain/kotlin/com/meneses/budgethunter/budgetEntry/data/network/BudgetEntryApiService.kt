package com.meneses.budgethunter.budgetEntry.data.network

import com.meneses.budgethunter.commons.data.network.models.BudgetEntryResponse
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetEntryRequest
import com.meneses.budgethunter.commons.data.network.models.UpdateBudgetEntryRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
        try {
            println("BudgetEntryApiService: Creating entry for budget $budgetId with request: $request")
            val response: BudgetEntryResponse = httpClient.post("/api/budgets/$budgetId/entries") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            println("BudgetEntryApiService: Successfully created entry: $response")
            Result.success(response)
        } catch (e: Exception) {
            println("BudgetEntryApiService: Error creating entry - ${e.message}")
            println("BudgetEntryApiService: Error type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(Exception("Failed to create entry on server: ${e.message}", e))
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
        try {
            println("BudgetEntryApiService: Updating entry $entryId for budget $budgetId with request: $request")
            val response: BudgetEntryResponse = httpClient.put("/api/budgets/$budgetId/entries/$entryId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            println("BudgetEntryApiService: Successfully updated entry: $response")
            Result.success(response)
        } catch (e: Exception) {
            println("BudgetEntryApiService: Error updating entry - ${e.message}")
            println("BudgetEntryApiService: Error type: ${e::class.simpleName}")
            e.printStackTrace()
            Result.failure(Exception("Failed to update entry on server: ${e.message}", e))
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
            try {
                println("BudgetEntryApiService: Fetching entries for budget $budgetId")
                val response = httpClient.get("/api/budgets/$budgetId/entries")
                val entries = response.body<List<BudgetEntryResponse>>()
                println("BudgetEntryApiService: Successfully fetched ${entries.size} entries")
                Result.success(entries)
            } catch (e: Exception) {
                println("BudgetEntryApiService: Error fetching entries - ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
}

package com.meneses.budgethunter.budgetList.data.network

import com.meneses.budgethunter.commons.data.network.models.BudgetResponse
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetRequest
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
            try {
                println("BudgetApiService: Creating budget with request: $request")
                val response: BudgetResponse = httpClient.post("/api/budgets/create_budget") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
                println("BudgetApiService: Successfully created budget: $response")
                Result.success(response)
            } catch (e: Exception) {
                println("BudgetApiService: Error creating budget - ${e.message}")
                println("BudgetApiService: Error type: ${e::class.simpleName}")
                e.printStackTrace()
                Result.failure(Exception("Failed to create budget on server: ${e.message}", e))
            }
        }

    /**
     * Fetches all budgets for the authenticated user from the server.
     *
     * @return Result containing list of budget responses or error
     */
    suspend fun getBudgets(): Result<List<BudgetResponse>> =
        withContext(ioDispatcher) {
            try {
                println("BudgetApiService: Fetching budgets from server")
                val response = httpClient.get("/api/budgets/get_budgets")
                val budgets = response.body<List<BudgetResponse>>()
                println("BudgetApiService: Successfully fetched ${budgets.size} budgets")
                Result.success(budgets)
            } catch (e: Exception) {
                println("BudgetApiService: Error fetching budgets - ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
}

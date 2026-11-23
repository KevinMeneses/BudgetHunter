package com.meneses.budgethunter.collaborator.data.network

import com.meneses.budgethunter.billing.domain.SharingLimitType
import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.models.AddCollaboratorRequest
import com.meneses.budgethunter.commons.data.network.models.CollaboratorResponse
import com.meneses.budgethunter.commons.data.network.models.SharingErrorResponse
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import com.meneses.budgethunter.commons.data.network.bodyOrThrow
import com.meneses.budgethunter.commons.data.network.safeApiCall
import com.meneses.budgethunter.commons.data.network.toApiError
import com.meneses.budgethunter.commons.data.sync.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * API service for collaborator-related network operations.
 * Handles communication with the backend for collaborator management using RESTful conventions.
 */
class CollaboratorApiService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger
) {
    private val tag = "CollaboratorApiService"

    /**
     * Adds a collaborator to a budget.
     *
     * RESTful endpoint: POST /api/budgets/{budgetId}/collaborators
     *
     * The status is checked before the body is read, so a refusal maps to its [ApiError]: 404 (no account with
     * that email) is [ApiError.NotFound], 409 (already a member) [ApiError.Conflict], and 402 past the owner's
     * sharing limit [ApiError.SharingLimitReached], with the limit hit and whether the caller owns the budget.
     *
     * @param budgetId Server-side budget ID (passed in URL path)
     * @param request Collaborator addition request with budgetId and email
     * @return Result containing the collaborator response or error
     */
    suspend fun addCollaborator(
        budgetId: Long,
        request: AddCollaboratorRequest
    ): Result<CollaboratorResponse> = withContext(ioDispatcher) {
        logger.debug(tag, "Adding a collaborator to budget $budgetId")
        safeApiCall {
            val response = try {
                httpClient.post(ApiEndpoints.budgetCollaborators(budgetId)) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            } catch (e: ClientRequestException) {
                // Only reached when the client is configured with expectSuccess; Ktor keeps the body
                // readable there, which the 402 needs (covered by CollaboratorApiServiceTest)
                e.response
            }
            if (!response.status.isSuccess()) throw response.toAddCollaboratorError()
            response.body<CollaboratorResponse>().also {
                logger.debug(tag, "Added a collaborator to budget ${it.budgetId}")
            }
        }
    }

    private suspend fun HttpResponse.toAddCollaboratorError(): ApiError {
        if (status == HttpStatusCode.PaymentRequired) {
            val detail = try {
                body<SharingErrorResponse>().takeIf { it.code == SHARING_LIMIT_REACHED }?.sharing
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(tag, "Unreadable sharing limit answer", e)
                null
            }
            if (detail != null) {
                logger.debug(tag, "Sharing limit reached: ${detail.limitType}, caller is owner: ${detail.callerIsOwner}")
                return ApiError.SharingLimitReached(
                    limitType = SharingLimitType.entries.firstOrNull { it.name == detail.limitType },
                    callerIsOwner = detail.callerIsOwner
                )
            }
        }
        logger.warn(tag, "Collaborator not added: HTTP ${status.value}")
        return status.toApiError()
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
            logger.debug(tag, "Fetching collaborators for budget $budgetId")
            safeApiCall {
                httpClient.get(ApiEndpoints.budgetCollaborators(budgetId))
                    .bodyOrThrow<List<UserInfo>>()
                    .also { logger.debug(tag, "Successfully fetched ${it.size} collaborators") }
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
        logger.debug(tag, "Removing a collaborator from budget $budgetId")
        safeApiCall {
            val encodedEmail = email.encodeURLPath()
            httpClient.delete(ApiEndpoints.budgetCollaborator(budgetId, encodedEmail)).bodyOrThrow<Unit>()
            logger.debug(tag, "Removed a collaborator from budget $budgetId")
        }
    }

    private companion object {
        const val SHARING_LIMIT_REACHED = "SHARING_LIMIT_REACHED"
    }
}

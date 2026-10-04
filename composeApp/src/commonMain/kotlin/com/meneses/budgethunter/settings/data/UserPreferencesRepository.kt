package com.meneses.budgethunter.settings.data

import com.meneses.budgethunter.commons.data.network.ApiEndpoints
import com.meneses.budgethunter.commons.data.network.models.UpdateUserPreferencesRequest
import com.meneses.budgethunter.commons.data.network.models.UserPreferencesResponse
import com.meneses.budgethunter.commons.data.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** The account-side copy of the settings preferences. */
class UserPreferencesRepository(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun get(): Result<UserPreferencesResponse> = withContext(ioDispatcher) {
        safeApiCall {
            httpClient.get(ApiEndpoints.PREFERENCES).body<UserPreferencesResponse>()
        }
    }

    suspend fun save(request: UpdateUserPreferencesRequest): Result<UserPreferencesResponse> =
        withContext(ioDispatcher) {
            safeApiCall {
                httpClient.put(ApiEndpoints.PREFERENCES) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body<UserPreferencesResponse>()
            }
        }
}

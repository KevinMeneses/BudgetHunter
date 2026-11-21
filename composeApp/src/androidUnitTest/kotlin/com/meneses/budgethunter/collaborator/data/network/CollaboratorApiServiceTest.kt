package com.meneses.budgethunter.collaborator.data.network

import com.meneses.budgethunter.billing.domain.SharingLimitType
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.models.AddCollaboratorRequest
import com.meneses.budgethunter.commons.data.network.models.CollaboratorResponse
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CollaboratorApiServiceTest {

    private val dispatcher = StandardTestDispatcher()
    private val request = AddCollaboratorRequest(budgetId = 7, email = "friend@example.com")

    private fun TestScope.service(
        status: HttpStatusCode,
        body: String,
        expectSuccess: Boolean = false
    ): CollaboratorApiService {
        val engine = MockEngine {
            respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        val client = HttpClient(engine) {
            this.expectSuccess = expectSuccess
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return CollaboratorApiService(client, StandardTestDispatcher(testScheduler), NoOpLogger())
    }

    private fun sharingLimit(limitType: String, callerIsOwner: Boolean) = """
        {"status":402,"code":"SHARING_LIMIT_REACHED","message":"This budget cannot have more collaborators",
         "sharing":{"limitType":"$limitType","plan":"FREE","limit":1,"used":1,"callerIsOwner":$callerIsOwner}}
    """.trimIndent()

    @Test
    fun `an addition reads the collaborator`() = runTest(dispatcher) {
        val body = """{"budgetId":7,"budgetName":"Casa","collaboratorEmail":"friend@example.com","collaboratorName":"Friend"}"""

        val result = service(HttpStatusCode.OK, body).addCollaborator(7, request)

        assertEquals(CollaboratorResponse(7, "Casa", "friend@example.com", "Friend"), result.getOrThrow())
    }

    @Test
    fun `the owner past the people per budget limit`() = runTest(dispatcher) {
        val result = service(HttpStatusCode.PaymentRequired, sharingLimit("COLLABORATORS_PER_BUDGET", true))
            .addCollaborator(7, request)

        assertEquals(
            ApiError.SharingLimitReached(SharingLimitType.COLLABORATORS_PER_BUDGET, callerIsOwner = true),
            result.exceptionOrNull()
        )
    }

    @Test
    fun `a member past the shared budgets limit`() = runTest(dispatcher) {
        val result = service(HttpStatusCode.PaymentRequired, sharingLimit("SHARED_BUDGETS", false))
            .addCollaborator(7, request)

        assertEquals(
            ApiError.SharingLimitReached(SharingLimitType.SHARED_BUDGETS, callerIsOwner = false),
            result.exceptionOrNull()
        )
    }

    @Test
    fun `the 402 is read too from a client that throws on errors`() = runTest(dispatcher) {
        val result = service(HttpStatusCode.PaymentRequired, sharingLimit("SHARED_BUDGETS", true), expectSuccess = true)
            .addCollaborator(7, request)

        assertEquals(
            ApiError.SharingLimitReached(SharingLimitType.SHARED_BUDGETS, callerIsOwner = true),
            result.exceptionOrNull()
        )
    }

    @Test
    fun `a limit this app does not know still is a sharing limit`() = runTest(dispatcher) {
        val result = service(HttpStatusCode.PaymentRequired, sharingLimit("SOMETHING_NEW", true))
            .addCollaborator(7, request)

        assertEquals(ApiError.SharingLimitReached(null, callerIsOwner = true), result.exceptionOrNull())
    }

    @Test
    fun `a 402 that is not a sharing limit is not mistaken for one`() = runTest(dispatcher) {
        val result = service(HttpStatusCode.PaymentRequired, """{"status":402,"code":"QUOTA_EXCEEDED"}""")
            .addCollaborator(7, request)

        assertEquals(ApiError.Unknown, result.exceptionOrNull())
    }

    @Test
    fun `an unknown account is not found`() = runTest(dispatcher) {
        val result = service(HttpStatusCode.NotFound, """{"status":404,"message":"User not found"}""")
            .addCollaborator(7, request)

        assertEquals(ApiError.NotFound, result.exceptionOrNull())
    }

    @Test
    fun `someone already in the budget is a conflict`() = runTest(dispatcher) {
        val result = service(HttpStatusCode.Conflict, """{"status":409,"message":"Already a collaborator"}""")
            .addCollaborator(7, request)

        assertEquals(ApiError.Conflict, result.exceptionOrNull())
    }

    @Test
    fun `404 and 409 read the same from a client that throws on errors`() = runTest(dispatcher) {
        assertEquals(
            ApiError.NotFound,
            service(HttpStatusCode.NotFound, "{}", expectSuccess = true).addCollaborator(7, request).exceptionOrNull()
        )
        assertEquals(
            ApiError.Conflict,
            service(HttpStatusCode.Conflict, "{}", expectSuccess = true).addCollaborator(7, request).exceptionOrNull()
        )
    }

    @Test
    fun `the members say who owns the budget, and older servers do not`() = runTest(dispatcher) {
        val body = """[{"email":"me@example.com","name":"Me","isOwner":true},{"email":"old@example.com","name":"Old"}]"""

        val members = service(HttpStatusCode.OK, body).getCollaborators(7).getOrThrow()

        assertEquals(
            listOf(UserInfo("me@example.com", "Me", isOwner = true), UserInfo("old@example.com", "Old", isOwner = false)),
            members
        )
    }
}

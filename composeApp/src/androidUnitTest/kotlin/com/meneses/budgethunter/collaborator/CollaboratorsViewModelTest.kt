package com.meneses.budgethunter.collaborator

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.collaborator_added_successfully
import budgethunter.composeapp.generated.resources.failed_to_add_collaborator
import budgethunter.composeapp.generated.resources.failed_to_load_collaborators
import budgethunter.composeapp.generated.resources.sharing_limit_collaborators
import budgethunter.composeapp.generated.resources.sharing_limit_generic
import budgethunter.composeapp.generated.resources.sharing_limit_not_owner
import budgethunter.composeapp.generated.resources.sharing_limit_shared_budgets
import com.meneses.budgethunter.billing.data.BillingApiException
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.billing.domain.SharingLimitType
import com.meneses.budgethunter.billing.domain.SharingStatus
import com.meneses.budgethunter.collaborator.application.CollaboratorsEvent
import com.meneses.budgethunter.collaborator.application.CollaboratorsIntent
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.models.CollaboratorResponse
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CollaboratorsViewModel event emissions.
 *
 * Verifies that the ViewModel emits one-shot ShowError and ShowSuccess events through
 * its Channel-backed `events` Flow when collaborator operations complete or fail.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollaboratorsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val budgetServerId = 101L

    // Mocks
    private val collaboratorRepository = mockk<CollaboratorRepository>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val premiumRepository = mockk<PremiumRepository>()

    // System under test
    private lateinit var viewModel: CollaboratorsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CollaboratorsViewModel(
            collaboratorRepository = collaboratorRepository,
            preferencesManager = preferencesManager,
            premiumRepository = premiumRepository,
            logger = NoOpLogger(),
            budgetServerId = budgetServerId
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== loadCollaborators failure Tests ==========

    @Test
    fun `loadCollaborators failure emits ShowError event`() = runTest {
        // Given - the API fails when loading collaborators
        val errorMessage = "Failed to connect to server"
        coEvery {
            collaboratorRepository.getCollaborators(budgetServerId)
        } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.sendIntent(CollaboratorsIntent.LoadCollaborators)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - a ShowError event is emitted with the localized error message
        val event = viewModel.events.first()
        assertIs<CollaboratorsEvent.ShowError>(event)
        assertEquals(Res.string.failed_to_load_collaborators, event.message)
    }

    // ========== addCollaborator success Tests ==========

    @Test
    fun `addCollaborator success emits ShowSuccess event`() = runTest {
        // Given - load collaborators succeeds and add collaborator succeeds
        val existingCollaborators = listOf(
            UserInfo(email = "owner@example.com", name = "Owner")
        )
        coEvery {
            collaboratorRepository.getCollaborators(budgetServerId)
        } returns Result.success(existingCollaborators)

        val successResponse = CollaboratorResponse(
            budgetId = budgetServerId,
            budgetName = "My Budget",
            collaboratorEmail = "friend@example.com",
            collaboratorName = "Friend"
        )
        coEvery {
            collaboratorRepository.addCollaborator(budgetServerId, "friend@example.com")
        } returns Result.success(successResponse)

        // When
        viewModel.sendIntent(CollaboratorsIntent.AddCollaborator("friend@example.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - a ShowSuccess event is emitted with the names to fill the message in
        val event = viewModel.events.first()
        assertIs<CollaboratorsEvent.ShowSuccess>(event)
        assertEquals(Res.string.collaborator_added_successfully, event.message)
        assertEquals(listOf("Friend", "My Budget"), event.formatArgs)
    }

    // ========== addCollaborator failure Tests ==========

    @Test
    fun `addCollaborator failure emits ShowError event`() = runTest {
        // Given - the API fails when adding a collaborator
        val errorMessage = "User not found"
        coEvery {
            collaboratorRepository.addCollaborator(budgetServerId, "unknown@example.com")
        } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.sendIntent(CollaboratorsIntent.AddCollaborator("unknown@example.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - a ShowError event is emitted with the localized error message
        val event = viewModel.events.first()
        assertIs<CollaboratorsEvent.ShowError>(event)
        assertEquals(Res.string.failed_to_add_collaborator, event.message)
    }

    // ========== currentUserEmail Tests ==========

    @Test
    fun `loadCollaborators puts the last signed in email in the state`() = runTest {
        coEvery { preferencesManager.getLastSignedInEmail() } returns "me@example.com"
        coEvery {
            collaboratorRepository.getCollaborators(budgetServerId)
        } returns Result.success(emptyList())

        viewModel.sendIntent(CollaboratorsIntent.LoadCollaborators)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("me@example.com", viewModel.uiState.value.currentUserEmail)
    }

    // ========== Sharing limit Tests ==========

    private suspend fun addRefusedWith(limitType: SharingLimitType?, callerIsOwner: Boolean): CollaboratorsEvent {
        coEvery {
            collaboratorRepository.addCollaborator(budgetServerId, "friend@example.com")
        } returns Result.failure(ApiError.SharingLimitReached(limitType, callerIsOwner))

        viewModel.sendIntent(CollaboratorsIntent.AddCollaborator("friend@example.com"))
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel.events.first()
    }

    @Test
    fun `the owner past the people per budget limit is offered Premium`() = runTest {
        val event = addRefusedWith(SharingLimitType.COLLABORATORS_PER_BUDGET, callerIsOwner = true)

        assertIs<CollaboratorsEvent.ShowSharingLimit>(event)
        assertEquals(Res.string.sharing_limit_collaborators, event.message)
        assertTrue(event.offersPremium)
    }

    @Test
    fun `the owner past the shared budgets limit is offered Premium`() = runTest {
        val event = addRefusedWith(SharingLimitType.SHARED_BUDGETS, callerIsOwner = true)

        assertIs<CollaboratorsEvent.ShowSharingLimit>(event)
        assertEquals(Res.string.sharing_limit_shared_budgets, event.message)
        assertTrue(event.offersPremium)
    }

    @Test
    fun `the owner past a limit this app does not know gets the general notice and Premium`() = runTest {
        val event = addRefusedWith(limitType = null, callerIsOwner = true)

        assertIs<CollaboratorsEvent.ShowSharingLimit>(event)
        assertEquals(Res.string.sharing_limit_generic, event.message)
        assertTrue(event.offersPremium)
    }

    @Test
    fun `a member past the people per budget limit is told it is the owner's plan, without Premium`() = runTest {
        val event = addRefusedWith(SharingLimitType.COLLABORATORS_PER_BUDGET, callerIsOwner = false)

        assertIs<CollaboratorsEvent.ShowSharingLimit>(event)
        assertEquals(Res.string.sharing_limit_not_owner, event.message)
        assertFalse(event.offersPremium)
    }

    @Test
    fun `a member past the shared budgets limit is told it is the owner's plan, without Premium`() = runTest {
        val event = addRefusedWith(SharingLimitType.SHARED_BUDGETS, callerIsOwner = false)

        assertIs<CollaboratorsEvent.ShowSharingLimit>(event)
        assertEquals(Res.string.sharing_limit_not_owner, event.message)
        assertFalse(event.offersPremium)
    }

    @Test
    fun `a refusal keeps the field open for another try`() = runTest {
        addRefusedWith(SharingLimitType.SHARED_BUDGETS, callerIsOwner = true)

        assertFalse(viewModel.uiState.value.isAddingCollaborator)
    }

    // ========== Sharing hint Tests ==========

    private val freeLimits = SharingStatus(sharedBudgetsLimit = 1, sharedBudgetsUsed = 0, collaboratorsPerBudgetLimit = 1)

    private fun membersWhereOwnerIs(owner: String) = listOf(
        UserInfo(email = "me@example.com", name = "Me", isOwner = owner == "me@example.com"),
        UserInfo(email = "other@example.com", name = "Other", isOwner = owner == "other@example.com")
    )

    private fun loadAs(members: List<UserInfo>) {
        coEvery { preferencesManager.getLastSignedInEmail() } returns "me@example.com"
        coEvery { collaboratorRepository.getCollaborators(budgetServerId) } returns Result.success(members)
        viewModel.sendIntent(CollaboratorsIntent.LoadCollaborators)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `the owner on a free plan sees its limits`() = runTest {
        coEvery { premiumRepository.sharingStatus() } returns freeLimits

        loadAs(membersWhereOwnerIs("me@example.com"))

        assertEquals(freeLimits, viewModel.uiState.value.ownerSharingLimits)
    }

    @Test
    fun `the owner on premium sees no limits`() = runTest {
        coEvery { premiumRepository.sharingStatus() } returns SharingStatus(null, 3, null)

        loadAs(membersWhereOwnerIs("me@example.com"))

        assertNull(viewModel.uiState.value.ownerSharingLimits)
    }

    @Test
    fun `a member sees no limits, and the plan is not even asked`() = runTest {
        loadAs(membersWhereOwnerIs("other@example.com"))

        assertNull(viewModel.uiState.value.ownerSharingLimits)
        coVerify(exactly = 0) { premiumRepository.sharingStatus() }
    }

    @Test
    fun `no hint when the plan cannot be read`() = runTest {
        coEvery { premiumRepository.sharingStatus() } throws BillingApiException(null, "offline")

        loadAs(membersWhereOwnerIs("me@example.com"))

        assertNull(viewModel.uiState.value.ownerSharingLimits)
        assertEquals(2, viewModel.uiState.value.collaborators.size)
    }

    @Test
    fun `no hint from a server that does not say who owns the budget`() = runTest {
        loadAs(membersWhereOwnerIs(owner = "nobody"))

        assertNull(viewModel.uiState.value.ownerSharingLimits)
    }
}

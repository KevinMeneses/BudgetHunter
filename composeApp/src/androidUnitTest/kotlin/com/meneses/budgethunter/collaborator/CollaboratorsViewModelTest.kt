package com.meneses.budgethunter.collaborator

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.collaborator_added_successfully
import budgethunter.composeapp.generated.resources.failed_to_add_collaborator
import budgethunter.composeapp.generated.resources.failed_to_load_collaborators
import com.meneses.budgethunter.collaborator.application.CollaboratorsEvent
import com.meneses.budgethunter.collaborator.application.CollaboratorsIntent
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import com.meneses.budgethunter.commons.data.network.models.CollaboratorResponse
import com.meneses.budgethunter.commons.data.network.models.UserInfo
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
import kotlin.test.assertIs

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

    // System under test
    private lateinit var viewModel: CollaboratorsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CollaboratorsViewModel(
            collaboratorRepository = collaboratorRepository,
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
}

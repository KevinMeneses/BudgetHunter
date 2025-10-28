package com.meneses.budgethunter.collaborator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.collaborator.application.CollaboratorsEvent
import com.meneses.budgethunter.collaborator.application.CollaboratorsState
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Collaborators screen.
 * Manages collaborator operations following the MVI pattern.
 *
 * @property collaboratorRepository Repository for collaborator data operations
 * @property budgetServerId Server-side ID of the budget to manage collaborators for
 */
class CollaboratorsViewModel(
    private val collaboratorRepository: CollaboratorRepository,
    private val budgetServerId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollaboratorsState())
    val uiState = _uiState.asStateFlow()

    /**
     * Handles events from the UI.
     *
     * @param event The event to handle
     */
    fun sendEvent(event: CollaboratorsEvent) {
        when (event) {
            is CollaboratorsEvent.LoadCollaborators -> loadCollaborators()
            is CollaboratorsEvent.ToggleAddCollaboratorDialog -> toggleAddCollaboratorDialog(event.show)
            is CollaboratorsEvent.AddCollaborator -> addCollaborator(event.email)
            is CollaboratorsEvent.ClearMessages -> clearMessages()
        }
    }

    /**
     * Loads the list of collaborators for the current budget from the server.
     */
    private fun loadCollaborators() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            collaboratorRepository.getCollaborators(budgetServerId)
                .onSuccess { collaborators ->
                    _uiState.update {
                        it.copy(
                            collaborators = collaborators,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load collaborators"
                        )
                    }
                }
        }
    }

    /**
     * Shows or hides the add collaborator dialog.
     *
     * @param show True to show the dialog, false to hide it
     */
    private fun toggleAddCollaboratorDialog(show: Boolean) {
        _uiState.update { it.copy(showAddCollaboratorDialog = show) }
    }

    /**
     * Adds a new collaborator to the budget by their email address.
     *
     * @param email Email address of the collaborator to add
     */
    private fun addCollaborator(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email cannot be empty") }
            return
        }

        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingCollaborator = true, errorMessage = null) }

            collaboratorRepository.addCollaborator(budgetServerId, email)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isAddingCollaborator = false,
                            showAddCollaboratorDialog = false,
                            successMessage = "Successfully added ${response.collaboratorName} to ${response.budgetName}"
                        )
                    }
                    // Reload collaborators to show the new one
                    loadCollaborators()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAddingCollaborator = false,
                            errorMessage = error.message ?: "Failed to add collaborator"
                        )
                    }
                }
        }
    }

    /**
     * Clears any error or success messages from the state.
     */
    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

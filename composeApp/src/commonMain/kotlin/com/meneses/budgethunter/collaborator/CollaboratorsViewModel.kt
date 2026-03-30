package com.meneses.budgethunter.collaborator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.collaborator.application.CollaboratorsEvent
import com.meneses.budgethunter.collaborator.application.CollaboratorsIntent
import com.meneses.budgethunter.collaborator.application.CollaboratorsState
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollaboratorsViewModel(
    private val collaboratorRepository: CollaboratorRepository,
    private val budgetServerId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollaboratorsState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<CollaboratorsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun sendIntent(intent: CollaboratorsIntent) {
        when (intent) {
            is CollaboratorsIntent.LoadCollaborators -> loadCollaborators()
            is CollaboratorsIntent.ToggleAddCollaboratorDialog -> toggleAddCollaboratorDialog(intent.show)
            is CollaboratorsIntent.AddCollaborator -> addCollaborator(intent.email)
            is CollaboratorsIntent.RemoveCollaborator -> removeCollaborator(intent.email)
            is CollaboratorsIntent.ToggleRemoveConfirmationDialog -> toggleRemoveConfirmationDialog(intent.email)
        }
    }

    private fun loadCollaborators() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

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
                    _uiState.update { it.copy(isLoading = false) }
                    _events.trySend(
                        CollaboratorsEvent.ShowError(
                            error.message ?: "Failed to load collaborators"
                        )
                    )
                }
        }
    }

    private fun toggleAddCollaboratorDialog(show: Boolean) {
        _uiState.update { it.copy(showAddCollaboratorDialog = show) }
    }

    private fun addCollaborator(email: String) {
        if (email.isBlank()) {
            _events.trySend(CollaboratorsEvent.ShowError("Email cannot be empty"))
            return
        }

        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            _events.trySend(CollaboratorsEvent.ShowError("Please enter a valid email address"))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingCollaborator = true) }

            collaboratorRepository.addCollaborator(budgetServerId, email)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isAddingCollaborator = false,
                            showAddCollaboratorDialog = false
                        )
                    }
                    _events.trySend(
                        CollaboratorsEvent.ShowSuccess(
                            "Successfully added ${response.collaboratorName} to ${response.budgetName}"
                        )
                    )
                    // Reload collaborators to show the new one
                    loadCollaborators()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isAddingCollaborator = false) }
                    _events.trySend(
                        CollaboratorsEvent.ShowError(
                            error.message ?: "Failed to add collaborator"
                        )
                    )
                }
        }
    }

    private fun toggleRemoveConfirmationDialog(email: String?) {
        _uiState.update { it.copy(removeConfirmationEmail = email) }
    }

    private fun removeCollaborator(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRemovingCollaborator = true) }

            collaboratorRepository.removeCollaborator(budgetServerId, email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isRemovingCollaborator = false,
                            removeConfirmationEmail = null
                        )
                    }
                    _events.trySend(
                        CollaboratorsEvent.ShowSuccess(
                            "Successfully removed $email from collaborators"
                        )
                    )
                    // Reload collaborators to reflect the removal
                    loadCollaborators()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isRemovingCollaborator = false) }
                    _events.trySend(
                        CollaboratorsEvent.ShowError(
                            error.message ?: "Failed to remove collaborator"
                        )
                    )
                }
        }
    }
}

package com.meneses.budgethunter.collaborator

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.collaborator_added_successfully
import budgethunter.composeapp.generated.resources.collaborator_removed_successfully
import budgethunter.composeapp.generated.resources.email_cannot_be_empty
import budgethunter.composeapp.generated.resources.failed_to_add_collaborator
import budgethunter.composeapp.generated.resources.failed_to_load_collaborators
import budgethunter.composeapp.generated.resources.failed_to_remove_collaborator
import budgethunter.composeapp.generated.resources.invalid_email_address
import budgethunter.composeapp.generated.resources.sharing_limit_collaborators
import budgethunter.composeapp.generated.resources.sharing_limit_generic
import budgethunter.composeapp.generated.resources.sharing_limit_not_owner
import budgethunter.composeapp.generated.resources.sharing_limit_shared_budgets
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.billing.data.PremiumRepository
import com.meneses.budgethunter.billing.domain.SharingLimitType
import com.meneses.budgethunter.collaborator.application.CollaboratorsEvent
import com.meneses.budgethunter.collaborator.application.CollaboratorsIntent
import com.meneses.budgethunter.collaborator.application.CollaboratorsState
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import com.meneses.budgethunter.commons.data.sync.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollaboratorsViewModel(
    private val collaboratorRepository: CollaboratorRepository,
    private val preferencesManager: PreferencesManager,
    private val premiumRepository: PremiumRepository,
    private val logger: Logger,
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
            // The screen is only reachable while signed in, so the last account is the current one
            val currentUserEmail = preferencesManager.getLastSignedInEmail()
            _uiState.update { it.copy(isLoading = true, currentUserEmail = currentUserEmail) }

            collaboratorRepository.getCollaborators(budgetServerId)
                .onSuccess { collaborators ->
                    _uiState.update {
                        it.copy(
                            collaborators = collaborators,
                            isLoading = false
                        )
                    }
                    updateSharingHint(collaborators, currentUserEmail)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.trySend(
                        CollaboratorsEvent.ShowError(Res.string.failed_to_load_collaborators)
                    )
                }
        }
    }

    /**
     * The owner of a budget on a plan with limits sees them before inviting. Read after the list, which says who
     * owns the budget; when the plan cannot be read the hint is just not shown (or keeps what it showed).
     */
    private suspend fun updateSharingHint(collaborators: List<UserInfo>, currentUserEmail: String?) {
        val isOwner = collaborators.any { it.isOwner && it.email.trim().equals(currentUserEmail?.trim(), ignoreCase = true) }
        if (!isOwner) {
            _uiState.update { it.copy(ownerSharingLimits = null) }
            return
        }
        val sharing = try {
            premiumRepository.sharingStatus()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(TAG, "Could not read the sharing limits", e)
            return
        }
        _uiState.update { it.copy(ownerSharingLimits = sharing?.takeIf { s -> s.isLimited }) }
    }

    private fun toggleAddCollaboratorDialog(show: Boolean) {
        _uiState.update { it.copy(showAddCollaboratorDialog = show) }
    }

    private fun addCollaborator(email: String) {
        if (email.isBlank()) {
            _events.trySend(CollaboratorsEvent.ShowError(Res.string.email_cannot_be_empty))
            return
        }

        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            _events.trySend(CollaboratorsEvent.ShowError(Res.string.invalid_email_address))
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
                            message = Res.string.collaborator_added_successfully,
                            formatArgs = listOf(response.collaboratorName, response.budgetName)
                        )
                    )
                    // Reload collaborators to show the new one
                    loadCollaborators()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isAddingCollaborator = false) }
                    _events.trySend(
                        if (error is ApiError.SharingLimitReached) {
                            sharingLimitEvent(error)
                        } else {
                            CollaboratorsEvent.ShowError(Res.string.failed_to_add_collaborator)
                        }
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
                            message = Res.string.collaborator_removed_successfully,
                            formatArgs = listOf(email)
                        )
                    )
                    // Reload collaborators to reflect the removal
                    loadCollaborators()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isRemovingCollaborator = false) }
                    _events.trySend(
                        CollaboratorsEvent.ShowError(Res.string.failed_to_remove_collaborator)
                    )
                }
        }
    }

    /** Only the owner is offered Premium: buying it does nothing for anyone else's budget. */
    private fun sharingLimitEvent(error: ApiError.SharingLimitReached): CollaboratorsEvent {
        if (!error.callerIsOwner) {
            return CollaboratorsEvent.ShowSharingLimit(Res.string.sharing_limit_not_owner, offersPremium = false)
        }
        val message = when (error.limitType) {
            SharingLimitType.COLLABORATORS_PER_BUDGET -> Res.string.sharing_limit_collaborators
            SharingLimitType.SHARED_BUDGETS -> Res.string.sharing_limit_shared_budgets
            null -> Res.string.sharing_limit_generic
        }
        return CollaboratorsEvent.ShowSharingLimit(message, offersPremium = true)
    }

    private companion object {
        const val TAG = "CollaboratorsViewModel"
    }
}

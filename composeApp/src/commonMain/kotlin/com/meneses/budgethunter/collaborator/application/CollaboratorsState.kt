package com.meneses.budgethunter.collaborator.application

import com.meneses.budgethunter.commons.data.network.models.UserInfo

/**
 * UI state for the Collaborators screen.
 *
 * @property collaborators List of current collaborators for the budget
 * @property isLoading True when loading collaborators from server
 * @property isAddingCollaborator True when adding a new collaborator
 * @property isRemovingCollaborator True when removing a collaborator
 * @property errorMessage Error message to display to user, null when no error
 * @property successMessage Success message to display to user, null when no success
 * @property showAddCollaboratorDialog True when the add collaborator dialog should be shown
 * @property removeConfirmationEmail Email of collaborator to remove (null when dialog is hidden)
 */
data class CollaboratorsState(
    val collaborators: List<UserInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isAddingCollaborator: Boolean = false,
    val isRemovingCollaborator: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showAddCollaboratorDialog: Boolean = false,
    val removeConfirmationEmail: String? = null
)

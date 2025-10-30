package com.meneses.budgethunter.collaborator.application

/**
 * Events that can be triggered from the Collaborators screen.
 */
sealed interface CollaboratorsEvent {
    /**
     * Load collaborators for the current budget from the server.
     */
    data object LoadCollaborators : CollaboratorsEvent

    /**
     * Show or hide the add collaborator dialog.
     *
     * @property show True to show the dialog, false to hide it
     */
    data class ToggleAddCollaboratorDialog(val show: Boolean) : CollaboratorsEvent

    /**
     * Add a new collaborator to the budget by their email address.
     *
     * @property email Email address of the collaborator to add
     */
    data class AddCollaborator(val email: String) : CollaboratorsEvent

    /**
     * Request removal of a collaborator from the budget.
     *
     * @property email Email address of the collaborator to remove
     */
    data class RemoveCollaborator(val email: String) : CollaboratorsEvent

    /**
     * Show or hide the remove collaborator confirmation dialog.
     *
     * @property email Email address of the collaborator to potentially remove (null to hide dialog)
     */
    data class ToggleRemoveConfirmationDialog(val email: String?) : CollaboratorsEvent

    /**
     * Clear any error or success messages.
     */
    data object ClearMessages : CollaboratorsEvent
}

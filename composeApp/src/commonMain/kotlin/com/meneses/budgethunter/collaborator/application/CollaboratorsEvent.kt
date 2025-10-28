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
     * Clear any error or success messages.
     */
    data object ClearMessages : CollaboratorsEvent
}

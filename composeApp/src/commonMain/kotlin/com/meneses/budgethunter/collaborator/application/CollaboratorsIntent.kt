package com.meneses.budgethunter.collaborator.application

sealed interface CollaboratorsIntent {
    data object LoadCollaborators : CollaboratorsIntent
    data class ToggleAddCollaboratorDialog(val show: Boolean) : CollaboratorsIntent
    data class AddCollaborator(val email: String) : CollaboratorsIntent
    data class RemoveCollaborator(val email: String) : CollaboratorsIntent
    data class ToggleRemoveConfirmationDialog(val email: String?) : CollaboratorsIntent
    data object ClearMessages : CollaboratorsIntent
}

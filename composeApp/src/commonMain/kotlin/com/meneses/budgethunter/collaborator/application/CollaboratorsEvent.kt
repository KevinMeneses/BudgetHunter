package com.meneses.budgethunter.collaborator.application

sealed interface CollaboratorsEvent {
    data class ShowError(val message: String) : CollaboratorsEvent
    data class ShowSuccess(val message: String) : CollaboratorsEvent
}

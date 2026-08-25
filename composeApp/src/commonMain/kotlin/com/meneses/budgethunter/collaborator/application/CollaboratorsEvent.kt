package com.meneses.budgethunter.collaborator.application

import org.jetbrains.compose.resources.StringResource

sealed interface CollaboratorsEvent {
    val message: StringResource
    val formatArgs: List<String>

    data class ShowError(
        override val message: StringResource,
        override val formatArgs: List<String> = emptyList()
    ) : CollaboratorsEvent

    data class ShowSuccess(
        override val message: StringResource,
        override val formatArgs: List<String> = emptyList()
    ) : CollaboratorsEvent
}

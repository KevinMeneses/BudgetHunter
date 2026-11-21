package com.meneses.budgethunter.collaborator.application

import org.jetbrains.compose.resources.StringResource

sealed interface CollaboratorsEvent {
    val message: StringResource
    val formatArgs: List<String>

    data class ShowError(
        override val message: StringResource,
        override val formatArgs: List<String> = emptyList()
    ) : CollaboratorsEvent

    /**
     * The owner's plan allows no more sharing. Not an error: [offersPremium] (only for the owner, whose upgrade
     * is the one that lifts the limit) adds a "See Premium" action.
     */
    data class ShowSharingLimit(
        override val message: StringResource,
        val offersPremium: Boolean
    ) : CollaboratorsEvent {
        override val formatArgs: List<String> = emptyList()
    }

    data class ShowSuccess(
        override val message: StringResource,
        override val formatArgs: List<String> = emptyList()
    ) : CollaboratorsEvent
}

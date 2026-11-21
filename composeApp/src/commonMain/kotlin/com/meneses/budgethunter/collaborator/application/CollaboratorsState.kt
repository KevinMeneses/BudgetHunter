package com.meneses.budgethunter.collaborator.application

import com.meneses.budgethunter.billing.domain.SharingStatus
import com.meneses.budgethunter.commons.data.network.models.UserInfo

/**
 * UI state for the Collaborators screen.
 *
 * @property collaborators List of current collaborators for the budget
 * @property isLoading True when loading collaborators from server
 * @property isAddingCollaborator True when adding a new collaborator
 * @property isRemovingCollaborator True when removing a collaborator
 * @property showAddCollaboratorDialog True when the add collaborator dialog should be shown
 * @property removeConfirmationEmail Email of collaborator to remove (null when dialog is hidden)
 */
data class CollaboratorsState(
    val collaborators: List<UserInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isAddingCollaborator: Boolean = false,
    val isRemovingCollaborator: Boolean = false,
    val showAddCollaboratorDialog: Boolean = false,
    val removeConfirmationEmail: String? = null,
    /** The signed-in user's email, to tell their own row apart from the others. */
    val currentUserEmail: String? = null,
    /**
     * The plan's sharing limits, shown as a hint to the budget's owner while the plan has any (FREE). Null for
     * members, for PREMIUM, and when the plan could not be read. Only informative: the server decides.
     */
    val ownerSharingLimits: SharingStatus? = null
)

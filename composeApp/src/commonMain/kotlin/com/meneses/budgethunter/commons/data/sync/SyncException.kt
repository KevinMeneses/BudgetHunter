package com.meneses.budgethunter.commons.data.sync

/**
 * Base exception for synchronization errors.
 */
sealed class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * User is not authenticated. Authentication is required for sync operations.
     */
    class NotAuthenticated(message: String = "User must be authenticated to sync") :
        SyncException(message)

    /**
     * Parent entity is not synced with server. Cannot sync child entities without parent server ID.
     * For example, cannot sync budget entries if the budget hasn't been synced to server yet.
     */
    class ParentNotSynced(message: String) : SyncException(message)
}

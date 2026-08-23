package com.meneses.budgethunter.commons.data.sync

/**
 * Result of a synchronization operation.
 * Tracks success, partial success (some items failed), or complete failure.
 */
sealed interface SyncResult<out T> {
    /**
     * All items synchronized successfully.
     */
    data class Success<T>(val data: T) : SyncResult<T>

    /**
     * Some items synchronized successfully, but some failed.
     * @param data The result data (e.g., sync statistics)
     * @param succeeded Number of items that synchronized successfully
     * @param failed Number of items that failed to synchronize
     * @param errors List of errors for failed items
     */
    data class PartialSuccess<T>(
        val data: T,
        val succeeded: Int,
        val failed: Int,
        val errors: List<SyncError>
    ) : SyncResult<T>

    /**
     * Synchronization failed completely.
     */
    data class Failure(val error: Throwable) : SyncResult<Nothing>
}

/**
 * Represents an error that occurred during synchronization of a specific item.
 * @param itemIdentifier Unique identifier for the item that failed (e.g., "Budget 123", "Entry 456")
 * @param error The error that occurred
 */
data class SyncError(
    val itemIdentifier: String,
    val error: Throwable
)

/**
 * Statistics about a synchronization operation.
 * @param totalItems Total number of items attempted to sync
 * @param syncedItems Number of items successfully synchronized
 * @param failedItems Number of items that failed to synchronize
 * @param errors List of errors for failed items
 */
data class SyncStats(
    val totalItems: Int,
    val syncedItems: Int = 0,
    val failedItems: Int = 0,
    val errors: List<SyncError> = emptyList()
) {
    /**
     * All items synchronized successfully.
     */
    val isFullSuccess: Boolean = totalItems > 0 && failedItems == 0

    /**
     * Some items synchronized, but some failed.
     */
    val isPartialSuccess: Boolean = syncedItems > 0 && failedItems > 0

    /**
     * No items to synchronize.
     */
    val isEmpty: Boolean = totalItems == 0
}

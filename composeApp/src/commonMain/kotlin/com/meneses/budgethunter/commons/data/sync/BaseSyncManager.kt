package com.meneses.budgethunter.commons.data.sync

import com.meneses.budgethunter.auth.data.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Base class for synchronization managers that provides common functionality
 * for bidirectional sync between local database and backend server.
 *
 * This class handles:
 * - Authentication checks
 * - Dispatcher management (deprecated - should be handled by repository layer)
 * - Common error handling
 * - Logging
 *
 * @param authRepository Repository for checking authentication status
 * @param ioDispatcher Coroutine dispatcher for IO operations
 * @param logger Logger for sync operations
 */
abstract class BaseSyncManager(
    protected val authRepository: AuthRepository,
    protected val ioDispatcher: CoroutineDispatcher,
    protected val logger: Logger
) {

    /**
     * Tag used for logging. Subclasses should override this with their class name.
     */
    protected abstract val logTag: String

    /**
     * Execute a sync operation with authentication check and error handling.
     *
     * This method:
     * 1. Checks if user is authenticated
     * 2. Executes the operation on IO dispatcher
     * 3. Catches and logs any exceptions
     * 4. Returns a Result wrapping the operation result
     *
     * @param operation The sync operation to execute
     * @return Result wrapping the operation result
     */
    protected suspend fun <T> authenticatedSync(
        operation: suspend () -> T
    ): Result<T> = withContext(ioDispatcher) {
        try {
            // Check authentication
            if (!authRepository.isAuthenticated()) {
                return@withContext Result.failure(SyncException.NotAuthenticated())
            }

            // Execute operation
            val result = operation()
            Result.success(result)
        } catch (e: Exception) {
            logger.error(logTag, "Sync operation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Aggregate results from multiple sync operations.
     *
     * This method processes a list of items, executing a sync operation for each,
     * and tracks which succeeded and which failed.
     *
     * @param items List of items to sync
     * @param itemIdentifier Function to get a unique identifier for each item (for error reporting)
     * @param syncOperation Function that syncs a single item
     * @return SyncStats with success/failure counts and error details
     */
    protected suspend fun <T> aggregateSync(
        items: List<T>,
        itemIdentifier: (T) -> String,
        syncOperation: suspend (T) -> Result<Unit>
    ): SyncStats = coroutineScope {
        val results = items.map { item ->
            async { item to syncOperation(item) }
        }.awaitAll()

        val errors = mutableListOf<SyncError>()
        var synced = 0

        results.forEach { (item, result) ->
            result.fold(
                onSuccess = {
                    synced++
                    logger.debug(logTag, "Successfully synced ${itemIdentifier(item)}")
                },
                onFailure = { error ->
                    errors.add(SyncError(itemIdentifier(item), error))
                    logger.warn(logTag, "Failed to sync ${itemIdentifier(item)}", error)
                }
            )
        }

        SyncStats(
            totalItems = items.size,
            syncedItems = synced,
            failedItems = errors.size,
            errors = errors
        )
    }

    /**
     * Convert SyncStats to appropriate SyncResult.
     *
     * @param stats The sync statistics
     * @return SyncResult.Success if all succeeded, PartialSuccess if some failed, or Failure if all failed
     */
    protected fun statsToResult(stats: SyncStats): SyncResult<SyncStats> {
        return when {
            stats.isEmpty -> {
                logger.info(logTag, "No items to sync")
                SyncResult.Success(stats)
            }
            stats.isFullSuccess -> {
                logger.info(logTag, "All ${stats.totalItems} items synced successfully")
                SyncResult.Success(stats)
            }
            stats.isPartialSuccess -> {
                logger.warn(
                    logTag,
                    "Partial sync: ${stats.syncedItems}/${stats.totalItems} succeeded, ${stats.failedItems} failed"
                )
                SyncResult.PartialSuccess(
                    data = stats,
                    succeeded = stats.syncedItems,
                    failed = stats.failedItems,
                    errors = stats.errors
                )
            }
            else -> {
                logger.error(logTag, "All ${stats.totalItems} items failed to sync")
                SyncResult.Failure(
                    Exception("Failed to sync all items: ${stats.errors.size} errors")
                )
            }
        }
    }
}

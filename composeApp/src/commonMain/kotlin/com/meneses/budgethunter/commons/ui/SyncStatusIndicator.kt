package com.meneses.budgethunter.commons.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.pending_sync
import budgethunter.composeapp.generated.resources.synced
import org.jetbrains.compose.resources.stringResource

/**
 * Displays a sync status indicator icon showing whether an item is synced with the server.
 *
 * @param isSynced Whether the item has been synced to the server
 * @param modifier Modifier to be applied to the icon
 */
@Composable
fun SyncStatusIndicator(
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = when {
            isSynced -> Icons.Default.CloudDone
            else -> Icons.Default.CloudQueue
        },
        contentDescription = stringResource(if (isSynced) Res.string.synced else Res.string.pending_sync),
        modifier = modifier,
        tint = when {
            isSynced -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        }
    )
}

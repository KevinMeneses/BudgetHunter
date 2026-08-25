package com.meneses.budgethunter.collaborator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.add
import budgethunter.composeapp.generated.resources.add_collaborator
import budgethunter.composeapp.generated.resources.add_collaborator_message
import budgethunter.composeapp.generated.resources.add_collaborators_description
import budgethunter.composeapp.generated.resources.adding
import budgethunter.composeapp.generated.resources.back_content_description
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.collaborator_email_placeholder
import budgethunter.composeapp.generated.resources.collaborators_title
import budgethunter.composeapp.generated.resources.email
import budgethunter.composeapp.generated.resources.no_collaborators_yet
import budgethunter.composeapp.generated.resources.remove
import budgethunter.composeapp.generated.resources.remove_collaborator
import budgethunter.composeapp.generated.resources.remove_collaborator_confirmation_message
import budgethunter.composeapp.generated.resources.removing
import com.meneses.budgethunter.collaborator.application.CollaboratorsIntent
import com.meneses.budgethunter.collaborator.application.CollaboratorsState
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import com.meneses.budgethunter.commons.ui.AppBar
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

/**
 * Route for the Collaborators screen.
 *
 * @property budgetServerId Server-side ID of the budget to manage collaborators for
 * @property budgetName Name of the budget (for display purposes)
 */
@Serializable
data class CollaboratorsScreen(
    val budgetServerId: Long,
    val budgetName: String
) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        uiState: CollaboratorsState,
        onIntent: (CollaboratorsIntent) -> Unit,
        snackbarHostState: SnackbarHostState,
        goBack: () -> Unit
    ) {
        // Load collaborators when screen opens
        DisposableEffect(Unit) {
            CollaboratorsIntent.LoadCollaborators.run(onIntent)
            onDispose { }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(Res.string.collaborators_title, budgetName),
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = stringResource(Res.string.back_content_description),
                    onLeftButtonClick = goBack
                )
            },
            floatingActionButton = {
                if (!uiState.isLoading) {
                    FloatingActionButton(
                        onClick = { CollaboratorsIntent.ToggleAddCollaboratorDialog(true).run(onIntent) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.add_collaborator)
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            CollaboratorsContent(
                paddingValues = paddingValues,
                uiState = uiState,
                onIntent = onIntent
            )
        }

        // Add Collaborator Dialog
        if (uiState.showAddCollaboratorDialog) {
            AddCollaboratorDialog(
                isLoading = uiState.isAddingCollaborator,
                onDismiss = { CollaboratorsIntent.ToggleAddCollaboratorDialog(false).run(onIntent) },
                onConfirm = { email ->
                    CollaboratorsIntent.AddCollaborator(email).run(onIntent)
                }
            )
        }

        // Remove Collaborator Confirmation Dialog
        uiState.removeConfirmationEmail?.let { emailToRemove ->
            RemoveCollaboratorConfirmationDialog(
                email = emailToRemove,
                isLoading = uiState.isRemovingCollaborator,
                onDismiss = { CollaboratorsIntent.ToggleRemoveConfirmationDialog(null).run(onIntent) },
                onConfirm = {
                    CollaboratorsIntent.RemoveCollaborator(emailToRemove).run(onIntent)
                }
            )
        }
    }
}

/**
 * Content area of the Collaborators screen.
 */
@Composable
private fun CollaboratorsContent(
    paddingValues: PaddingValues,
    uiState: CollaboratorsState,
    onIntent: (CollaboratorsIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when {
            uiState.isLoading -> {
                // Show loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.collaborators.isEmpty() -> {
                // Show empty state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = stringResource(Res.string.no_collaborators_yet),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.add_collaborators_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // Show collaborators list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.collaborators) { collaborator ->
                        CollaboratorCard(
                            collaborator = collaborator,
                            onRemoveClick = {
                                CollaboratorsIntent.ToggleRemoveConfirmationDialog(collaborator.email).run(onIntent)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a single collaborator's information.
 */
@Composable
private fun CollaboratorCard(
    collaborator: UserInfo,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collaborator.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = collaborator.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.padding(end = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.remove_collaborator),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Dialog for adding a new collaborator.
 */
@Composable
private fun AddCollaboratorDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(Res.string.add_collaborator)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.add_collaborator_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.email)) },
                    placeholder = { Text(stringResource(Res.string.collaborator_email_placeholder)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email) },
                enabled = !isLoading && email.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(stringResource(if (isLoading) Res.string.adding else Res.string.add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Confirmation dialog for removing a collaborator.
 */
@Composable
private fun RemoveCollaboratorConfirmationDialog(
    email: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(Res.string.remove_collaborator)) },
        text = {
            Text(
                text = stringResource(Res.string.remove_collaborator_confirmation_message, email),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
                Text(stringResource(if (isLoading) Res.string.removing else Res.string.remove))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

// Extension function for cleaner event handling
private fun CollaboratorsIntent.run(onIntent: (CollaboratorsIntent) -> Unit) = onIntent(this)

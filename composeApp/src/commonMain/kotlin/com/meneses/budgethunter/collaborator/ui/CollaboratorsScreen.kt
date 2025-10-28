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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.back_content_description
import com.meneses.budgethunter.collaborator.application.CollaboratorsEvent
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
        onEvent: (CollaboratorsEvent) -> Unit,
        goBack: () -> Unit
    ) {
        val snackBarHostState = remember { SnackbarHostState() }

        // Load collaborators when screen opens
        DisposableEffect(Unit) {
            CollaboratorsEvent.LoadCollaborators.run(onEvent)
            onDispose { }
        }

        // Show snackbar for error messages
        LaunchedEffect(key1 = uiState.errorMessage) {
            uiState.errorMessage?.let { message ->
                snackBarHostState.showSnackbar(message)
                CollaboratorsEvent.ClearMessages.run(onEvent)
            }
        }

        // Show snackbar for success messages
        LaunchedEffect(key1 = uiState.successMessage) {
            uiState.successMessage?.let { message ->
                snackBarHostState.showSnackbar(message)
                CollaboratorsEvent.ClearMessages.run(onEvent)
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = "Collaborators - $budgetName",
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = stringResource(Res.string.back_content_description),
                    onLeftButtonClick = goBack
                )
            },
            floatingActionButton = {
                if (!uiState.isLoading) {
                    FloatingActionButton(
                        onClick = { CollaboratorsEvent.ToggleAddCollaboratorDialog(true).run(onEvent) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add collaborator"
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState)
            }
        ) { paddingValues ->
            CollaboratorsContent(
                paddingValues = paddingValues,
                uiState = uiState
            )
        }

        // Add Collaborator Dialog
        if (uiState.showAddCollaboratorDialog) {
            AddCollaboratorDialog(
                isLoading = uiState.isAddingCollaborator,
                onDismiss = { CollaboratorsEvent.ToggleAddCollaboratorDialog(false).run(onEvent) },
                onConfirm = { email ->
                    CollaboratorsEvent.AddCollaborator(email).run(onEvent)
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
    uiState: CollaboratorsState
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
                        text = "No collaborators yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add collaborators to share this budget",
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
                        CollaboratorCard(collaborator)
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
private fun CollaboratorCard(collaborator: UserInfo) {
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
        title = { Text("Add Collaborator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter the email address of the person you want to add as a collaborator:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("collaborator@example.com") },
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
                Text(if (isLoading) "Adding..." else "Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

// Extension function for cleaner event handling
private fun CollaboratorsEvent.run(onEvent: (CollaboratorsEvent) -> Unit) = onEvent(this)

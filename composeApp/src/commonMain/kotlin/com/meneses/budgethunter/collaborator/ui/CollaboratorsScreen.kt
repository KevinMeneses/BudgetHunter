package com.meneses.budgethunter.collaborator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.add_collaborators_description
import budgethunter.composeapp.generated.resources.back_content_description
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.collaborator_email_placeholder
import budgethunter.composeapp.generated.resources.collaborators_explainer
import budgethunter.composeapp.generated.resources.collaborators_screen_title
import budgethunter.composeapp.generated.resources.email
import budgethunter.composeapp.generated.resources.invite
import budgethunter.composeapp.generated.resources.no_collaborators_yet
import budgethunter.composeapp.generated.resources.people_count
import budgethunter.composeapp.generated.resources.remove
import budgethunter.composeapp.generated.resources.remove_collaborator
import budgethunter.composeapp.generated.resources.remove_collaborator_confirmation_message
import budgethunter.composeapp.generated.resources.removing
import budgethunter.composeapp.generated.resources.you_suffix
import com.meneses.budgethunter.collaborator.application.CollaboratorsIntent
import com.meneses.budgethunter.collaborator.application.CollaboratorsState
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.theme.AppColors
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.pluralStringResource
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
        DisposableEffect(Unit) {
            CollaboratorsIntent.LoadCollaborators.run(onIntent)
            onDispose { }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(Res.string.collaborators_screen_title),
                    subtitle = budgetName,
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = stringResource(Res.string.back_content_description),
                    onLeftButtonClick = goBack
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            CollaboratorsContent(
                paddingValues = paddingValues,
                budgetName = budgetName,
                uiState = uiState,
                onIntent = onIntent
            )
        }

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
 * What sharing does, the field to invite someone, then the people who share the budget. When
 * nobody else is in it yet, a note says why one would invite someone.
 */
@Composable
private fun CollaboratorsContent(
    paddingValues: PaddingValues,
    budgetName: String,
    uiState: CollaboratorsState,
    onIntent: (CollaboratorsIntent) -> Unit
) {
    val isMe = { user: UserInfo ->
        uiState.currentUserEmail?.trim()?.equals(user.email.trim(), ignoreCase = true) == true
    }
    val hasOthers = uiState.collaborators.any { !isMe(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "explainer") {
            Text(
                text = stringResource(Res.string.collaborators_explainer, budgetName),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = AppColors.onSurfaceVariant
            )
        }

        item(key = "invite") {
            InviteField(
                isAdding = uiState.isAddingCollaborator,
                collaboratorCount = uiState.collaborators.size,
                onInvite = { CollaboratorsIntent.AddCollaborator(it).run(onIntent) }
            )
        }

        if (uiState.isLoading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        if (uiState.collaborators.isNotEmpty()) {
            item(key = "count") {
                Text(
                    text = pluralStringResource(
                        Res.plurals.people_count,
                        uiState.collaborators.size,
                        uiState.collaborators.size
                    ).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    color = AppColors.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
            item(key = "people") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(AppColors.surfaceContainer)
                ) {
                    uiState.collaborators.forEachIndexed { index, collaborator ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                color = AppColors.outlineVariant.copy(alpha = 0.6f)
                            )
                        }
                        CollaboratorRow(
                            collaborator = collaborator,
                            isMe = isMe(collaborator),
                            onRemoveClick = {
                                CollaboratorsIntent.ToggleRemoveConfirmationDialog(collaborator.email).run(onIntent)
                            }
                        )
                    }
                }
            }
        }

        if (!hasOthers) {
            item(key = "empty") {
                EmptyNote()
            }
        }
    }
}

@Composable
private fun InviteField(
    isAdding: Boolean,
    collaboratorCount: Int,
    onInvite: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    // Cleared once the list grows, which is when the invitation went through
    LaunchedEffect(collaboratorCount) { email = "" }

    val canInvite = email.isNotBlank() && !isAdding
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(Res.string.email)) },
            placeholder = { Text(stringResource(Res.string.collaborator_email_placeholder)) },
            singleLine = true,
            enabled = !isAdding,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { if (canInvite) onInvite(email) })
        )
        Button(
            onClick = { onInvite(email) },
            enabled = canInvite,
            modifier = Modifier
                .padding(top = 8.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.onPrimary
                )
            } else {
                Text(stringResource(Res.string.invite))
            }
        }
    }
}

@Composable
private fun CollaboratorRow(
    collaborator: UserInfo,
    isMe: Boolean,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppColors.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = collaborator.name.ifBlank { collaborator.email }.take(1).uppercase(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.onTertiaryContainer
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append(collaborator.name.ifBlank { collaborator.email })
                    if (isMe) append(" ").append(stringResource(Res.string.you_suffix))
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = collaborator.email,
                fontSize = 13.sp,
                color = AppColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Nobody removes themselves from here: the row of the current user has no action
        if (!isMe) {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = stringResource(Res.string.remove_collaborator),
                    tint = AppColors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, AppColors.outlineVariant, MaterialTheme.shapes.large)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = AppColors.outline,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = stringResource(Res.string.no_collaborators_yet),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.add_collaborators_description),
            fontSize = 13.sp,
            color = AppColors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
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
                colors = ButtonDefaults.buttonColors(
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

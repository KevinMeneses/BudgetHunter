package com.meneses.budgethunter.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.already_have_account
import budgethunter.composeapp.generated.resources.confirm_password
import budgethunter.composeapp.generated.resources.create_account
import budgethunter.composeapp.generated.resources.email
import budgethunter.composeapp.generated.resources.hide_password
import budgethunter.composeapp.generated.resources.name
import budgethunter.composeapp.generated.resources.password
import budgethunter.composeapp.generated.resources.show_password
import budgethunter.composeapp.generated.resources.sign_up
import com.meneses.budgethunter.auth.application.SignUpEvent
import com.meneses.budgethunter.auth.application.SignUpState
import com.meneses.budgethunter.commons.ui.LoadingOverlay
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
object SignUpScreen {
    @Composable
    fun Show(
        uiState: SignUpState,
        onEvent: (SignUpEvent) -> Unit,
        navigateToSignIn: () -> Unit
    ) {
        LaunchedEffect(uiState.isSignedUp) {
            if (uiState.isSignedUp) {
                navigateToSignIn()
            }
        }

        Scaffold { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(Res.string.create_account),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Error Alert
                    uiState.error?.let { errorResource ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = stringResource(errorResource),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onEvent(SignUpEvent.DismissError) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { onEvent(SignUpEvent.EmailChanged(it)) },
                        label = { Text(stringResource(Res.string.email)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { onEvent(SignUpEvent.NameChanged(it)) },
                        label = { Text(stringResource(Res.string.name)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { onEvent(SignUpEvent.PasswordChanged(it)) },
                        label = { Text(stringResource(Res.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible)
                                        stringResource(Res.string.hide_password)
                                    else
                                        stringResource(Res.string.show_password)
                                )
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var confirmPasswordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = { onEvent(SignUpEvent.ConfirmPasswordChanged(it)) },
                        label = { Text(stringResource(Res.string.confirm_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onEvent(SignUpEvent.SignUpClicked) }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible)
                                        stringResource(Res.string.hide_password)
                                    else
                                        stringResource(Res.string.show_password)
                                )
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onEvent(SignUpEvent.SignUpClicked) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.email.isNotBlank() &&
                                uiState.name.isNotBlank() &&
                                uiState.password.isNotBlank() &&
                                uiState.confirmPassword.isNotBlank()
                    ) {
                        Text(stringResource(Res.string.sign_up))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { navigateToSignIn() }
                    ) {
                        Text(stringResource(Res.string.already_have_account))
                    }
                }

                if (uiState.isLoading) {
                    LoadingOverlay()
                }
            }
        }
    }
}

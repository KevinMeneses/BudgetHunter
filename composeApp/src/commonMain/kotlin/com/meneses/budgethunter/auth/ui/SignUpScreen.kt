package com.meneses.budgethunter.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
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
import com.meneses.budgethunter.auth.application.SignUpEvent
import com.meneses.budgethunter.auth.application.SignUpState
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.LoadingOverlay
import kotlinx.serialization.Serializable

@Serializable
object SignUpScreen {
    @Composable
    fun Show(
        uiState: SignUpState,
        onEvent: (SignUpEvent) -> Unit,
        navigateToSignIn: () -> Unit
    ) {
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.isSignedUp) {
            if (uiState.isSignedUp) {
                navigateToSignIn()
            }
        }

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it)
                onEvent(SignUpEvent.DismissError)
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = "Sign Up",
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = "Back",
                    onLeftButtonClick = { onEvent(SignUpEvent.NavigateToSignIn) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
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
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { onEvent(SignUpEvent.EmailChanged(it)) },
                        label = { Text("Email") },
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
                        label = { Text("Name") },
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
                        label = { Text("Password") },
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
                                        "Hide password"
                                    else
                                        "Show password"
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
                        label = { Text("Confirm Password") },
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
                                        "Hide password"
                                    else
                                        "Show password"
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
                        Text("Sign Up")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { onEvent(SignUpEvent.NavigateToSignIn) }
                    ) {
                        Text("Already have an account? Sign In")
                    }
                }

                if (uiState.isLoading) {
                    LoadingOverlay()
                }
            }
        }
    }
}

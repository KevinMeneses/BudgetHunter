package com.meneses.budgethunter.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.back_content_description
import budgethunter.composeapp.generated.resources.continue_with_email
import budgethunter.composeapp.generated.resources.email
import budgethunter.composeapp.generated.resources.no_account_prompt
import budgethunter.composeapp.generated.resources.sign_in
import budgethunter.composeapp.generated.resources.sign_in_title
import budgethunter.composeapp.generated.resources.sign_in_with_google
import budgethunter.composeapp.generated.resources.sign_up_link
import budgethunter.composeapp.generated.resources.use_without_account
import budgethunter.composeapp.generated.resources.welcome_tagline
import com.meneses.budgethunter.auth.application.SignInIntent
import com.meneses.budgethunter.auth.application.SignInState
import com.meneses.budgethunter.commons.ui.LoadingOverlay
import com.meneses.budgethunter.commons.ui.PlatformBackHandler
import com.meneses.budgethunter.theme.AppColors
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

/**
 * The way in, in two steps: a welcome that only asks how to enter (Google, email or without an
 * account), and behind "email" the short sign in form. Both are states of this screen, so coming
 * back from sign up lands on the form again.
 */
@Serializable
object SignInScreen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        uiState: SignInState,
        onIntent: (SignInIntent) -> Unit,
        navigateToSignUp: () -> Unit,
        canNavigateBack: Boolean = false,
        onNavigateBack: () -> Unit = {}
    ) {
        var showEmailForm by rememberSaveable { mutableStateOf(false) }
        val closeEmailForm = {
            showEmailForm = false
            onIntent(SignInIntent.DismissError)
        }
        PlatformBackHandler(enabled = showEmailForm, onBack = closeEmailForm)

        Scaffold(
            topBar = {
                if (showEmailForm || canNavigateBack) {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = if (showEmailForm) closeEmailForm else onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(Res.string.back_content_description)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.surface)
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (showEmailForm) {
                    EmailSignInForm(
                        uiState = uiState,
                        onIntent = onIntent,
                        navigateToSignUp = navigateToSignUp
                    )
                } else {
                    Welcome(
                        uiState = uiState,
                        onIntent = onIntent,
                        onContinueWithEmail = {
                            onIntent(SignInIntent.DismissError)
                            showEmailForm = true
                        }
                    )
                }

                if (uiState.isLoading) {
                    LoadingOverlay()
                }
            }
        }
    }

    @Composable
    private fun Welcome(
        uiState: SignInState,
        onIntent: (SignInIntent) -> Unit,
        onContinueWithEmail: () -> Unit
    ) {
        // No scroll: the welcome is short enough for any phone, and the weight below needs a
        // bounded height to push the choices down
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            AppMark()
            Spacer(modifier = Modifier.height(24.dp))
            AuthTitle(text = stringResource(Res.string.welcome_tagline))

            // Pushes the choices to the bottom, within reach of the thumb
            Spacer(modifier = Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.error?.let { error ->
                    AuthErrorBanner(
                        message = stringResource(error),
                        onDismiss = { onIntent(SignInIntent.DismissError) }
                    )
                }

                if (uiState.isGoogleAvailable) {
                    // The main way in, so the filled button; email follows as the secondary one
                    Button(
                        onClick = { onIntent(SignInIntent.GoogleSignInClicked) },
                        enabled = !uiState.isLoading,
                        modifier = AuthButtonModifier,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.onSurface,
                            contentColor = AppColors.surface
                        )
                    ) {
                        Text(stringResource(Res.string.sign_in_with_google), fontSize = 16.sp)
                    }
                    OutlinedButton(
                        onClick = onContinueWithEmail,
                        modifier = AuthButtonModifier
                    ) {
                        Text(
                            text = stringResource(Res.string.continue_with_email),
                            fontSize = 16.sp,
                            color = AppColors.onSurface
                        )
                    }
                } else {
                    // Built without Google: email is the only account option
                    Button(
                        onClick = onContinueWithEmail,
                        modifier = AuthButtonModifier
                    ) {
                        Text(stringResource(Res.string.continue_with_email), fontSize = 16.sp)
                    }
                }

                TextButton(
                    onClick = { onIntent(SignInIntent.ContinueOfflineClicked) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.use_without_account),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    @Composable
    private fun EmailSignInForm(
        uiState: SignInState,
        onIntent: (SignInIntent) -> Unit,
        navigateToSignUp: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthTitle(
                text = stringResource(Res.string.sign_in_title),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            uiState.error?.let { error ->
                AuthErrorBanner(
                    message = stringResource(error),
                    onDismiss = { onIntent(SignInIntent.DismissError) }
                )
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onIntent(SignInIntent.EmailChanged(it)) },
                label = { Text(stringResource(Res.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            PasswordField(
                value = uiState.password,
                onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
                imeAction = ImeAction.Done,
                onImeAction = { onIntent(SignInIntent.SignInClicked) }
            )

            Button(
                onClick = { onIntent(SignInIntent.SignInClicked) },
                modifier = Modifier.padding(top = 8.dp).then(AuthButtonModifier),
                enabled = uiState.email.isNotBlank() && uiState.password.isNotBlank()
            ) {
                Text(stringResource(Res.string.sign_in), fontSize = 16.sp)
            }

            AuthSwitchPrompt(
                question = stringResource(Res.string.no_account_prompt),
                action = stringResource(Res.string.sign_up_link),
                onClick = navigateToSignUp
            )
        }
    }
}

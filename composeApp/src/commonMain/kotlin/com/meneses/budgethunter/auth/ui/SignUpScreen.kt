package com.meneses.budgethunter.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.back_content_description
import budgethunter.composeapp.generated.resources.create_account_button
import budgethunter.composeapp.generated.resources.email
import budgethunter.composeapp.generated.resources.have_account_prompt
import budgethunter.composeapp.generated.resources.name
import budgethunter.composeapp.generated.resources.password_min_hint
import budgethunter.composeapp.generated.resources.sign_in_link
import budgethunter.composeapp.generated.resources.sign_up_title
import com.meneses.budgethunter.auth.application.SignUpIntent
import com.meneses.budgethunter.auth.application.SignUpState
import com.meneses.budgethunter.commons.ui.LoadingOverlay
import com.meneses.budgethunter.theme.AppColors
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

/**
 * Sign up with email: name, email and password, nothing else. Google lives on the welcome, so it
 * is not repeated here; there is no confirmation field, the eye shows the password to check it.
 * A validation error shows under its field; any other one in the banner on top.
 */
@Serializable
object SignUpScreen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        uiState: SignUpState,
        onIntent: (SignUpIntent) -> Unit
    ) {
        val nameError = uiState.nameError
        val emailError = uiState.emailError
        val passwordError = uiState.passwordError
        val generalError = uiState.error

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { onIntent(SignUpIntent.NavigateBack) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.back_content_description)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.surface)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                        text = stringResource(Res.string.sign_up_title),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    generalError?.let {
                        AuthErrorBanner(
                            message = stringResource(it),
                            onDismiss = { onIntent(SignUpIntent.DismissError) }
                        )
                    }

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { onIntent(SignUpIntent.NameChanged(it)) },
                        label = { Text(stringResource(Res.string.name)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(stringResource(it)) } },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                        label = { Text(stringResource(Res.string.email)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(stringResource(it)) } },
                        singleLine = true
                    )

                    // The minimum shows from the start, not only once it failed
                    PasswordField(
                        value = uiState.password,
                        onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                        imeAction = ImeAction.Done,
                        onImeAction = { onIntent(SignUpIntent.SignUpClicked) },
                        supportingText = stringResource(Res.string.password_min_hint),
                        error = passwordError?.let { stringResource(it) }
                    )

                    Button(
                        onClick = { onIntent(SignUpIntent.SignUpClicked) },
                        modifier = Modifier.padding(top = 8.dp).then(AuthButtonModifier),
                        enabled = !uiState.isLoading
                    ) {
                        Text(stringResource(Res.string.create_account_button), fontSize = 16.sp)
                    }

                    AuthSwitchPrompt(
                        question = stringResource(Res.string.have_account_prompt),
                        action = stringResource(Res.string.sign_in_link),
                        onClick = { onIntent(SignUpIntent.NavigateBack) }
                    )
                }

                if (uiState.isLoading) {
                    LoadingOverlay()
                }
            }
        }
    }
}

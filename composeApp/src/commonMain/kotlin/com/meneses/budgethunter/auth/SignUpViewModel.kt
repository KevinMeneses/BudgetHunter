package com.meneses.budgethunter.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.error_email_required
import budgethunter.composeapp.generated.resources.error_google_no_account
import budgethunter.composeapp.generated.resources.error_google_sign_in_failed
import budgethunter.composeapp.generated.resources.error_name_required
import budgethunter.composeapp.generated.resources.error_password_required
import budgethunter.composeapp.generated.resources.error_password_too_short
import budgethunter.composeapp.generated.resources.error_sign_up_failed
import com.meneses.budgethunter.auth.application.GoogleAuthOutcome
import com.meneses.budgethunter.auth.application.SignInWithGoogleUseCase
import com.meneses.budgethunter.auth.application.SignUpEvent
import com.meneses.budgethunter.auth.application.SignUpIntent
import com.meneses.budgethunter.auth.application.SignUpState
import com.meneses.budgethunter.auth.data.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class SignUpViewModel(
    private val authRepository: AuthRepository,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(
        SignUpState(isGoogleAvailable = signInWithGoogleUseCase.isAvailable)
    )

    private val _events = Channel<SignUpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun sendIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.EmailChanged -> updateEmail(intent.email)
            is SignUpIntent.NameChanged -> updateName(intent.name)
            is SignUpIntent.PasswordChanged -> updatePassword(intent.password)
            is SignUpIntent.SignUpClicked -> signUp()
            is SignUpIntent.GoogleSignUpClicked -> signUpWithGoogle()
            is SignUpIntent.DismissError -> dismissError()
            is SignUpIntent.NavigateBack -> _events.trySend(SignUpEvent.NavigateToSignIn)
        }
    }

    // Editing a field clears its own error, the others stay until the next attempt
    private fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    private fun signUp() {
        val currentState = _uiState.value

        // Every field is checked at once, so all its problems show in one attempt, each under its
        // field. There is no confirmation field: the eye icon shows the password to check it.
        val nameError = Res.string.error_name_required.takeIf { currentState.name.isBlank() }
        val emailError = Res.string.error_email_required.takeIf { currentState.email.isBlank() }
        val passwordError = when {
            currentState.password.isBlank() -> Res.string.error_password_required
            currentState.password.length < MIN_PASSWORD_LENGTH -> Res.string.error_password_too_short
            else -> null
        }
        if (nameError != null || emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError, error = null)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // Autocomplete often leaves a trailing space; the password is sent exactly as typed
            authRepository.signUp(
                email = currentState.email.trim(),
                name = currentState.name.trim(),
                password = currentState.password
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.trySend(SignUpEvent.NavigateToSignIn)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = Res.string.error_sign_up_failed
                        )
                    }
                }
            )
        }
    }

    private fun signUpWithGoogle() {
        // A second tap can land before Compose recomposes the button as disabled. The platform SDKs
        // do not queue concurrent sign-ins: on iOS the first flow's completion is simply dropped,
        // which would leave its coroutine suspended forever and the spinner stuck on screen.
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val outcome = signInWithGoogleUseCase.execute()) {
                // The server creates the account on first use, so signing up through Google
                // leaves the user signed in - there is no sign-in step to send them back to.
                is GoogleAuthOutcome.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.trySend(SignUpEvent.NavigateToBudgetList(outcome.email))
                }
                GoogleAuthOutcome.Cancelled -> _uiState.update { it.copy(isLoading = false) }
                GoogleAuthOutcome.NoGoogleAccount -> showError(Res.string.error_google_no_account)
                GoogleAuthOutcome.Failed -> showError(Res.string.error_google_sign_in_failed)
            }
        }
    }

    private fun showError(error: StringResource) {
        _uiState.update { it.copy(isLoading = false, error = error) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

private const val MIN_PASSWORD_LENGTH = 6

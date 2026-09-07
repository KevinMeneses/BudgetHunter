package com.meneses.budgethunter.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.error_google_no_account
import budgethunter.composeapp.generated.resources.error_google_sign_in_failed
import budgethunter.composeapp.generated.resources.error_sign_in_failed
import com.meneses.budgethunter.auth.application.GoogleAuthOutcome
import com.meneses.budgethunter.auth.application.SignInEvent
import com.meneses.budgethunter.auth.application.SignInIntent
import com.meneses.budgethunter.auth.application.SignInState
import com.meneses.budgethunter.auth.application.SignInWithGoogleUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class SignInViewModel(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    private val budgetRepository: BudgetRepository,
    private val budgetEntrySyncManager: BudgetEntrySyncManager,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(
        SignInState(isGoogleAvailable = signInWithGoogleUseCase.isAvailable)
    )

    private val _events = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun sendIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.EmailChanged -> updateEmail(intent.email)
            is SignInIntent.PasswordChanged -> updatePassword(intent.password)
            is SignInIntent.SignInClicked -> signIn()
            is SignInIntent.GoogleSignInClicked -> signInWithGoogle()
            is SignInIntent.DismissError -> dismissError()
            is SignInIntent.ContinueOfflineClicked -> continueOffline()
        }
    }

    private fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    private fun signIn() {
        val currentState = _uiState.value

        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(error = Res.string.error_sign_in_failed) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signIn(
                email = currentState.email,
                password = currentState.password
            ).fold(
                onSuccess = {
                    // Trigger background sync
                    launch {
                        // Sync all budgets (push local, then pull from server)
                        budgetRepository.sync()
                        // Sync all entries for all budgets (push local, then pull from server)
                        budgetEntrySyncManager.syncAllBudgetsEntries()
                    }

                    onAuthenticated(currentState.email)
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = Res.string.error_sign_in_failed
                        )
                    }
                }
            )
        }
    }

    private fun signInWithGoogle() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val outcome = signInWithGoogleUseCase.execute()) {
                is GoogleAuthOutcome.Success -> onAuthenticated(outcome.email)
                // Dismissing the account picker is the most common way out of this flow.
                // Showing an error for it would make the screen feel broken.
                GoogleAuthOutcome.Cancelled -> _uiState.update { it.copy(isLoading = false) }
                GoogleAuthOutcome.NoGoogleAccount -> showError(Res.string.error_google_no_account)
                GoogleAuthOutcome.Failed -> showError(Res.string.error_google_sign_in_failed)
            }
        }
    }

    private fun onAuthenticated(email: String) {
        _uiState.update { it.copy(isLoading = false) }
        _events.trySend(SignInEvent.NavigateToBudgetList(email))
    }

    private fun showError(error: StringResource) {
        _uiState.update { it.copy(isLoading = false, error = error) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun continueOffline() {
        viewModelScope.launch {
            // Save offline mode preference
            preferencesManager.setOfflineModeEnabled(true)
            _events.trySend(SignInEvent.NavigateToBudgetList(email = ""))
        }
    }
}

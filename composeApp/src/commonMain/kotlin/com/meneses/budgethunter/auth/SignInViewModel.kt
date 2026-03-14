package com.meneses.budgethunter.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.error_sign_in_failed
import com.meneses.budgethunter.auth.application.SignInIntent
import com.meneses.budgethunter.auth.application.SignInState
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    private val budgetRepository: BudgetRepository,
    private val budgetEntrySyncManager: BudgetEntrySyncManager
) : ViewModel() {

    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(SignInState())

    fun sendIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.EmailChanged -> updateEmail(intent.email)
            is SignInIntent.PasswordChanged -> updatePassword(intent.password)
            is SignInIntent.SignInClicked -> signIn()
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

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = true
                        )
                    }
                },
                onFailure = { exception ->
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

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun continueOffline() {
        viewModelScope.launch {
            // Save offline mode preference
            preferencesManager.setOfflineModeEnabled(true)
            _uiState.update { it.copy(continueOffline = true) }
        }
    }
}

package com.meneses.budgethunter.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carries the platform's request (a tapped transaction notification) to open the default
 * budget, or the settings when there is no default budget yet. It is consumed by the
 * navigation once the user is past splash and sign in.
 */
class DefaultBudgetLaunchRequest {
    private val _pending = MutableStateFlow(false)
    val pending = _pending.asStateFlow()

    fun request() {
        _pending.value = true
    }

    fun consume() {
        _pending.value = false
    }

    private val _settingsPending = MutableStateFlow(false)
    val settingsPending = _settingsPending.asStateFlow()

    /** Asks to open the settings, where the default budget is chosen. */
    fun requestSettings() {
        _settingsPending.value = true
    }

    fun consumeSettings() {
        _settingsPending.value = false
    }
}

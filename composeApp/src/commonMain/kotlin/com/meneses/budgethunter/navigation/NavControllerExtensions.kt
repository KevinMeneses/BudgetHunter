package com.meneses.budgethunter.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptions

/**
 * Navigates only when the current destination is settled, ignoring the extra taps of a
 * double click.
 *
 * Every tap sends an intent that ends up emitting a navigation event, and those events are
 * queued, so two fast taps used to push the same screen twice. The destination that starts
 * the navigation leaves the RESUMED state as soon as the first navigation is in flight,
 * which is what this guard checks.
 */
fun NavController.navigateOnce(route: Any, navOptions: NavOptions? = null) {
    val isCurrentDestinationSettled = currentBackStackEntry
        ?.lifecycle
        ?.currentState
        ?.isAtLeast(Lifecycle.State.RESUMED)
        ?: true

    if (isCurrentDestinationSettled) {
        navigate(route = route, navOptions = navOptions)
    }
}

/**
 * Pops the back stack only when [from] is still the destination on top of it, ignoring the
 * extra taps of a double click.
 *
 * The screen that asks to go back stays composed and clickable while the pop animation runs,
 * so two fast taps used to pop two screens. The pop is applied to the back stack right away,
 * which is what this guard checks, so it also works while the app is not resumed.
 */
fun NavController.popBackStackOnce(from: NavBackStackEntry) {
    if (currentBackStackEntry?.id == from.id) {
        popBackStack()
    }
}

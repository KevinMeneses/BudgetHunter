package com.meneses.budgethunter.commons.resources

import org.jetbrains.compose.resources.StringResource

/**
 * Provides string resources outside of Composable context.
 * This allows ViewModels, UseCases, and other non-UI code to access localized strings.
 */
interface StringResourceProvider {
    suspend fun getString(resource: StringResource): String
    suspend fun getString(resource: StringResource, vararg formatArgs: Any): String
}

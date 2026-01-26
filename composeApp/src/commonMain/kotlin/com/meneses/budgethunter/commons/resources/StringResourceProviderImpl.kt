package com.meneses.budgethunter.commons.resources

import org.jetbrains.compose.resources.StringResource

/**
 * Common implementation of StringResourceProvider for Compose Multiplatform.
 * Uses the platform-agnostic getString() from Compose resources.
 */
class StringResourceProviderImpl : StringResourceProvider {

    override suspend fun getString(resource: StringResource): String {
        return org.jetbrains.compose.resources.getString(resource)
    }

    override suspend fun getString(resource: StringResource, vararg formatArgs: Any): String {
        return org.jetbrains.compose.resources.getString(resource, *formatArgs)
    }
}

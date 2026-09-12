package com.meneses.budgethunter.commons.data.network

/**
 * Type-safe API endpoint constants and path builders.
 * Centralises all backend route strings to prevent typos and make refactoring easier.
 */
object ApiEndpoints {
    // Auth endpoints
    const val SIGN_UP = "/api/users/sign_up"
    const val SIGN_IN = "/api/users/sign_in"
    const val REFRESH_TOKEN = "/api/users/refresh_token"
    const val SIGN_IN_WITH_GOOGLE = "/api/users/sign_in_with_google"
    const val ME = "/api/users/me"
    const val PASSWORD = "/api/users/password"

    // Budget endpoints
    const val BUDGETS = "/api/budgets"
    fun budget(id: Long) = "/api/budgets/$id"

    // Budget entry endpoints
    fun budgetEntries(budgetId: Long) = "/api/budgets/$budgetId/entries"
    fun budgetEntry(budgetId: Long, entryId: Long) = "/api/budgets/$budgetId/entries/$entryId"
    fun budgetEntriesStream(budgetId: Long) = "/api/budgets/$budgetId/entries/stream"

    // Collaborator endpoints
    fun budgetCollaborators(budgetId: Long) = "/api/budgets/$budgetId/collaborators"
    fun budgetCollaborator(budgetId: Long, encodedEmail: String) = "/api/budgets/$budgetId/collaborators/$encodedEmail"
}

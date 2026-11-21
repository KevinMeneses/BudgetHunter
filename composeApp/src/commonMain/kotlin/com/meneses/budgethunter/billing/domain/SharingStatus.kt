package com.meneses.budgethunter.billing.domain

/**
 * What the account can share as a budget owner, as the server sees it. Only budgets the account owns count;
 * being invited is free. A null limit means unlimited (PREMIUM). The app only shows it: the server decides
 * every addition.
 */
data class SharingStatus(
    val sharedBudgetsLimit: Int?,
    /** Can be above [sharedBudgetsLimit]: what was shared before stays shared. */
    val sharedBudgetsUsed: Int,
    /** Members besides the owner each budget may have. */
    val collaboratorsPerBudgetLimit: Int?
) {
    val isLimited: Boolean get() = sharedBudgetsLimit != null || collaboratorsPerBudgetLimit != null
}

/** Which sharing limit stopped an addition. */
enum class SharingLimitType { SHARED_BUDGETS, COLLABORATORS_PER_BUDGET }

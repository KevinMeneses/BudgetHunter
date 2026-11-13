package com.meneses.budgethunter.auth.data

/** What the server answered to `DELETE /api/users/me`. */
enum class DeleteAccountResult {
    /** 204: the account and its tokens are gone. */
    DELETED,

    /**
     * 401: the token is missing, invalid or belongs to an account that no longer exists (a retry
     * after a 204 whose answer was lost also gets here). Either way this session is over.
     */
    UNAUTHORIZED,

    /** No connection, a timeout or any other status: nothing was deleted as far as the app knows. */
    FAILED
}

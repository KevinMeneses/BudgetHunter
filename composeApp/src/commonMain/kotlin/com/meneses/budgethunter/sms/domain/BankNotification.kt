package com.meneses.budgethunter.sms.domain

/**
 * A notification posted by a bank's app, read on Android by the notification listener.
 *
 * Apps split the movement between the title and the text, and either can hold the amount:
 * Nu writes "Recibiste 2.000,00 en tu cuenta" as the title and who sent it in the text, while
 * Nequi titles it "Envío" and writes "VALENTINA ARCINIEGAS te envió 1000" in the text.
 */
data class BankNotification(
    val title: String,
    val text: String
) {
    /** Title and text read as one message, the title first as the opening sentence. */
    val message: String
        get() = listOf(title.trim().trimEnd('.'), text.trim())
            .filter { it.isNotEmpty() }
            .joinToString(". ")

    /**
     * The text says who the money came from or went to, which the title rarely does, so it
     * describes the entry better. The title stands in when the app posts no text.
     */
    val description: String?
        get() = SmsTransactionParser.firstSentence(text) ?: SmsTransactionParser.firstSentence(title)

    val isBlank: Boolean
        get() = title.isBlank() && text.isBlank()
}

/**
 * Remembers the last notifications handled, because apps post the same one again when they
 * update it (a heads-up followed by a silent copy, an expanded text arriving later) and each
 * repost must not become another entry. Callers identify a notification by what survives an
 * update (on Android, its key and its time), never by its text.
 */
class RecentNotifications(private val capacity: Int = DEFAULT_CAPACITY) {

    private val seen = ArrayDeque<String>()

    /** True the first time [fingerprint] is seen, false while it is among the last remembered. */
    fun markIfNew(fingerprint: String): Boolean {
        if (fingerprint in seen) return false
        seen.addLast(fingerprint)
        if (seen.size > capacity) seen.removeFirst()
        return true
    }

    private companion object {
        const val DEFAULT_CAPACITY = 50
    }
}

interface BankNotificationService {
    suspend fun processBankNotification(notification: BankNotification, bankConfig: BankSmsConfig)
}

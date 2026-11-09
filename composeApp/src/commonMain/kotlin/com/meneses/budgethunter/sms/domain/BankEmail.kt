package com.meneses.budgethunter.sms.domain

/**
 * An email from a bank, as the notification of an email app shows it on Android: who wrote,
 * the subject and the start of the body. Some banks report movements only by email, and the
 * notification is the one part of it the app can read without access to the mailbox.
 */
data class BankEmail(
    val sender: String,
    val subject: String,
    val body: String
) {
    /**
     * Read like a bank app notification: the subject opens the message and the body, which says
     * where the money went ("Bancolombia le informa Compra por $50.000 en EXITO"), describes the
     * entry. Subjects are often generic ("Alertas y Notificaciones") and then only the body counts.
     */
    fun toNotification(): BankNotification = BankNotification(title = subject, text = body)

    val isBlank: Boolean
        get() = subject.isBlank() && body.isBlank()

    companion object {
        /**
         * The email apps whose notifications are looked at. Only the sender is read to tell
         * whether a selected bank wrote; the rest of any other email is never read.
         */
        val EMAIL_APP_PACKAGES = setOf(
            "com.google.android.gm", // Gmail
            "com.microsoft.office.outlook", // Outlook
            "com.samsung.android.email.provider", // Samsung Email
            "com.yahoo.mobile.client.android.mail", // Yahoo Mail
            "ch.protonmail.android", // Proton Mail
            "com.readdle.spark", // Spark
            "me.bluemail.mail", // BlueMail
            "org.kman.AquaMail", // Aqua Mail
            "com.easilydo.mail" // Edison Mail
        )

        fun isEmailApp(packageName: String?): Boolean = packageName in EMAIL_APP_PACKAGES

        /**
         * Builds the email from the parts of the notification. Email apps put the sender in the
         * title and the subject in the text, and the expanded text repeats the subject on its first
         * line before the start of the body, so that line is taken off the body.
         */
        fun fromNotification(title: String, text: String, bigText: String?): BankEmail {
            val subject = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            val fullText = bigText?.takeIf { it.isNotBlank() } ?: text
            val body = fullText.trim().let { content ->
                if (subject.isNotEmpty() && content.startsWith(subject)) content.removePrefix(subject) else content
            }
            return BankEmail(
                sender = title.trim(),
                subject = subject,
                // The body comes in lines cut by the app; one line reads as the message it is
                body = body.replace(WHITESPACE, " ").trim()
            )
        }

        /** The bank among [banks] that wrote the email, or null when none of them is the sender. */
        fun findSender(sender: String, banks: Collection<BankSmsConfig>): BankSmsConfig? =
            banks.firstOrNull { it.isSender(sender) }

        private val WHITESPACE = Regex("""\s+""")
    }
}

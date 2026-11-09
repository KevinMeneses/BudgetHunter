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

        /**
         * The bank among [banks] that sent the email, or null when none of them did.
         *
         * The display name ([sender]) is written by whoever sends the email, so a phishing email can
         * call itself "Bancolombia". When the email app also tells the sender's address
         * ([senderAddresses]), the address decides: an email from a bank's domain is that bank's,
         * whatever its name says, and a bank's name on an address outside its domains is a fake.
         * Only a bank whose domains are not known yet is still told by its name. Apps that give no
         * address leave the name as the only clue.
         */
        fun findSender(
            sender: String,
            senderAddresses: Collection<String>,
            banks: Collection<BankSmsConfig>
        ): BankSmsConfig? {
            val domains = senderAddresses.mapNotNull { addressDomain(it) }
            if (domains.isEmpty()) return banks.firstOrNull { it.isSender(sender) }

            return banks.firstOrNull { bank -> domains.any { bank.sendsEmailFrom(it) } }
                ?: banks.firstOrNull { it.emailDomains.isEmpty() && it.isSender(sender) }
        }

        /**
         * The domain of an email address as notifications carry it: "alertas@bancolombia.com.co",
         * "mailto:alertas@bancolombia.com.co" or "Bancolombia <alertas@bancolombia.com.co>".
         * Null for anything that is not an address, like a phone number or a contact URI.
         */
        fun addressDomain(address: String): String? {
            val domain = address.trim()
                .removePrefix("mailto:")
                .substringAfterLast('@', missingDelimiterValue = "")
                .substringBefore('>')
                .substringBefore('?')
                .trim()
                .trimEnd('.')
                .lowercase()
            val isDomain = domain.contains('.') &&
                domain.all { char -> char.isLetterOrDigit() || char == '.' || char == '-' }
            return domain.takeIf { isDomain }
        }

        private val WHITESPACE = Regex("""\s+""")
    }
}

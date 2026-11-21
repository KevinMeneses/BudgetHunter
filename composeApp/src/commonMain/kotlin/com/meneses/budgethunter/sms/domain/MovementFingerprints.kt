package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

/**
 * Identifies the movements already registered from notifications, so that one posted again is
 * not registered twice. Email apps post an email again while it is in the shade (Gmail does when
 * it syncs), and after the app's process dies (some devices kill it for memory often, and a
 * reboot does too) nothing in memory remembers it was read. The fingerprints are kept on disk
 * ([com.meneses.budgethunter.sms.data.RegisteredMovements]) as hashes, never the text: it holds
 * names and amounts.
 */
object MovementFingerprints {

    /** Enough for days of movements, while the stored list stays a few kilobytes. */
    const val CAPACITY = 200

    /**
     * A movement read from an email, by what it says rather than by the notification that showed
     * it: the same email posted again under another id or time, or shown by two email apps with
     * the same mailbox, is the same movement. Banks write the date and time of the movement in
     * the message, so two real movements read alike only if they also happen at the same time.
     */
    fun ofEmailMovement(bankConfig: BankSmsConfig, entry: BudgetEntry): String =
        hash(listOf("email", bankConfig.id, entry.date, entry.type.name, entry.amount, entry.description).joinToString("|"))

    /**
     * A bank app notification, by what identifies it on the device ([identity]: on Android its key
     * and its time). Its text is left out: bank apps do not always write the time, and two equal
     * transfers ("te envió 1000") are two movements.
     */
    fun ofAppNotification(identity: String): String = hash("app|$identity")

    /**
     * The [stored] fingerprints with [fingerprint] added, the oldest dropped past [capacity], or
     * null when it is already among them.
     */
    fun remember(stored: String?, fingerprint: String, capacity: Int = CAPACITY): String? {
        val fingerprints = stored?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()
        if (fingerprint in fingerprints) return null
        return (fingerprints + fingerprint).takeLast(capacity).joinToString(SEPARATOR.toString())
    }

    /** 64-bit FNV-1a: plain Kotlin on every platform, and collisions are negligible among 200. */
    private fun hash(text: String): String {
        var hash = FNV_OFFSET_BASIS
        for (byte in text.encodeToByteArray()) {
            hash = (hash xor byte.toUByte().toULong()) * FNV_PRIME
        }
        return hash.toString(radix = 16)
    }

    private const val SEPARATOR = ','
    private const val FNV_OFFSET_BASIS = 0xcbf29ce484222325UL
    private const val FNV_PRIME = 0x100000001b3UL
}

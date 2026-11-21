package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
     * it: the same email posted again under another id or time is the same movement (so is one
     * shown by two email apps with the same mailbox, as long as both show the same text). [message] is the email's whole text as shown (subject
     * and body), not the entry's description, which keeps only the first sentence, nor the part the
     * movement was read from: the time a bank writes after the movement still tells two apart.
     *
     * The day is the email's ([receivedAt], when the email app tells it), not the entry's, which is
     * the day it was read: an email posted again the next day is the same movement, while one that
     * repeats another day's text is a new one. Two movements of the same day whose emails write no
     * time and read alike (same amount, same shop) are taken as one: the price of not adding the
     * same email twice.
     */
    fun ofEmailMovement(bankConfig: BankSmsConfig, message: String, entry: BudgetEntry, receivedAt: Long?): String {
        val day = receivedAt
            ?.takeIf { it > 0 }
            ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() }
            ?: entry.date
        return hash(listOf("email", bankConfig.id, day, entry.type.name, entry.amount, message).joinToString("|"))
    }

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
        if (isRemembered(stored, fingerprint)) return null
        return (split(stored) + fingerprint).takeLast(capacity).joinToString(SEPARATOR.toString())
    }

    fun isRemembered(stored: String?, fingerprint: String): Boolean = fingerprint in split(stored)

    private fun split(stored: String?): List<String> = stored?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()

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

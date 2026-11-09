package com.meneses.budgethunter.sms.domain

/** A way a bank tells about a movement, each read by its own switch in Settings (Android). */
enum class ReadingChannel {
    SMS,

    /** The notifications of the bank's own app. */
    APP,

    /** The notification of an email app announcing an email from the bank. */
    EMAIL
}

/**
 * Which channels each selected bank is read from. A bank can tell about some movements by one
 * channel and others by another, so the user picks the channels per bank; one movement reported
 * by two of them is then registered twice, which Settings warns about.
 *
 * Kept on the device as "bankId:CHANNEL" strings. A bank with nothing stored is read the way it was
 * before channels could be picked: its SMS and its app, never its emails.
 */
object BankChannels {

    /** The channels a bank can be read from: its emails always, its SMS and app when it has them. */
    fun available(bank: BankSmsConfig): List<ReadingChannel> = buildList {
        if (bank.readsSms) add(ReadingChannel.SMS)
        if (bank.readsNotifications) add(ReadingChannel.APP)
        add(ReadingChannel.EMAIL)
    }

    fun default(bank: BankSmsConfig): Set<ReadingChannel> = buildSet {
        if (bank.readsSms) add(ReadingChannel.SMS)
        if (bank.readsNotifications) add(ReadingChannel.APP)
    }

    /** The channels [bank] is read from, out of what [chosen] holds for it. */
    fun of(bank: BankSmsConfig, chosen: Map<String, Set<ReadingChannel>>): Set<ReadingChannel> {
        val picked = chosen[bank.id]?.intersect(available(bank).toSet())
        return if (picked.isNullOrEmpty()) default(bank) else picked
    }

    /** The banks among [banks] read from [channel]. */
    fun readBy(
        channel: ReadingChannel,
        banks: Collection<BankSmsConfig>,
        chosen: Map<String, Set<ReadingChannel>>
    ): List<BankSmsConfig> = banks.filter { channel in of(it, chosen) }

    fun decode(stored: Set<String>): Map<String, Set<ReadingChannel>> =
        stored.mapNotNull { entry ->
            val bankId = entry.substringBefore(SEPARATOR, missingDelimiterValue = "")
            val channel = ReadingChannel.entries.find { it.name == entry.substringAfter(SEPARATOR) }
            if (bankId.isEmpty() || channel == null) null else bankId to channel
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }

    fun encode(chosen: Map<String, Set<ReadingChannel>>): Set<String> =
        chosen.flatMap { (bankId, channels) -> channels.map { "$bankId$SEPARATOR${it.name}" } }.toSet()

    private const val SEPARATOR = ':'
}

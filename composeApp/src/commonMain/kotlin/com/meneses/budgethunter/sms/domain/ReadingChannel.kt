package com.meneses.budgethunter.sms.domain

/** A way a bank tells about a movement (Android). */
enum class ReadingChannel {
    SMS,

    /** The notifications of the bank's own app. */
    APP,

    /** The notification of an email app announcing an email from the bank. */
    EMAIL
}

/**
 * Which channels each selected bank is read from: the one source of truth for automatic reading.
 * A bank can tell about some movements by one channel and others by another, so several can be
 * picked; one movement reported by two of them is then registered twice, which Settings warns about.
 *
 * Kept on the device as "bankId:CHANNEL" strings, with "bankId:" for a bank kept selected but read
 * from no channel. A bank with nothing stored (one that arrived from the account, say) is read by
 * its defaults: its SMS and its app, never its emails.
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
        val picked = chosen[bank.id] ?: return default(bank)
        return picked.intersect(available(bank).toSet())
    }

    /** The banks among [banks] read from [channel]. */
    fun readBy(
        channel: ReadingChannel,
        banks: Collection<BankSmsConfig>,
        chosen: Map<String, Set<ReadingChannel>>
    ): List<BankSmsConfig> = banks.filter { channel in of(it, chosen) }

    /** Every channel some of [banks] is read from: what needs a permission or a reader running. */
    fun inUse(banks: Collection<BankSmsConfig>, chosen: Map<String, Set<ReadingChannel>>): Set<ReadingChannel> =
        banks.flatMapTo(mutableSetOf()) { of(it, chosen) }

    /**
     * The channels of each selected bank as the three switches of earlier versions left them: a
     * bank was read from a channel only while that channel's switch was on. Run once, so updating
     * starts or stops reading nothing.
     */
    fun migrate(
        selectedBanks: Collection<BankSmsConfig>,
        stored: Map<String, Set<ReadingChannel>>,
        isSmsOn: Boolean,
        isAppOn: Boolean,
        isEmailOn: Boolean
    ): Map<String, Set<ReadingChannel>> = selectedBanks.associate { bank ->
        bank.id to of(bank, stored).filterTo(mutableSetOf()) { channel ->
            when (channel) {
                ReadingChannel.SMS -> isSmsOn
                ReadingChannel.APP -> isAppOn
                ReadingChannel.EMAIL -> isEmailOn
            }
        }
    }

    fun decode(stored: Set<String>): Map<String, Set<ReadingChannel>> {
        val chosen = mutableMapOf<String, MutableSet<ReadingChannel>>()
        for (entry in stored) {
            val bankId = entry.substringBefore(SEPARATOR, missingDelimiterValue = "")
            if (bankId.isEmpty()) continue
            val channelName = entry.substringAfter(SEPARATOR)
            val channel = ReadingChannel.entries.find { it.name == channelName }
            // An unknown channel (from a later version, say) is skipped rather than read as "none"
            if (channel == null && channelName.isNotEmpty()) continue
            val channels = chosen.getOrPut(bankId) { mutableSetOf() }
            channel?.let { channels += it }
        }
        return chosen
    }

    fun encode(chosen: Map<String, Set<ReadingChannel>>): Set<String> =
        chosen.flatMap { (bankId, channels) ->
            if (channels.isEmpty()) listOf("$bankId$SEPARATOR") else channels.map { "$bankId$SEPARATOR${it.name}" }
        }.toSet()

    private const val SEPARATOR = ':'
}

package com.meneses.budgethunter.sms.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.BankChannels
import com.meneses.budgethunter.sms.domain.ReadingChannel
import com.meneses.budgethunter.sms.domain.SupportedBanks
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.sms.domain.SmsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class SmsBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val smsService: SmsService by inject()
    private val preferencesManager: PreferencesManager by inject()
    private val ioScope: CoroutineScope by inject(named("IOScope"))

    override fun onReceive(context: Context, intent: Intent) {
        ioScope.launch {
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return@launch
            val selectedBankIds = preferencesManager.getSelectedBankIds()
            if (selectedBankIds.isEmpty()) return@launch

            // Only the banks the user reads by SMS: another channel may already register their movements
            val selectedBankConfigs = BankChannels.readBy(
                ReadingChannel.SMS,
                selectedBankIds.mapNotNull { bankId -> SupportedBanks.getBankConfigById(bankId) },
                preferencesManager.getBankChannels()
            ).toSet()

            if (selectedBankConfigs.isEmpty()) return@launch

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return@launch

            // A message longer than a single SMS arrives split in several parts. Each part on its
            // own is unreadable, so the parts of every sender are joined back into one message.
            messages
                .groupBy { it.originatingAddress }
                .forEach { (sender, parts) ->
                    val messageBody = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
                    processSmsMessage(messageBody, sender, selectedBankConfigs)
                }
        }
    }

    private suspend fun processSmsMessage(messageBody: String, sender: String?, bankConfigs: Set<BankSmsConfig>) {
        val isFromSelectedBank = sender != null && bankConfigs.any { bankConfig ->
            bankConfig.senderKeywords.any { keyword ->
                sender.contains(keyword, ignoreCase = true) || messageBody.contains(keyword, ignoreCase = true)
            }
        }

        if (isFromSelectedBank) {
            smsService.processSms(messageBody, bankConfigs)
        }
    }
}

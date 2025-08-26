package com.meneses.budgethunter.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import com.meneses.budgethunter.commons.bank.SupportedBanks
import com.meneses.budgethunter.commons.data.PreferencesManager
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
            if (!preferencesManager.isSmsReadingEnabled()) return@launch

            val selectedBankIds = preferencesManager.getSelectedBankIds()
            if (selectedBankIds.isEmpty()) return@launch

            val selectedBankConfigs = selectedBankIds.mapNotNull { bankId ->
                SupportedBanks.getBankConfigById(bankId)
            }.toSet()

            if (selectedBankConfigs.isEmpty()) return@launch

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { smsMessage ->
                val sender = smsMessage.originatingAddress
                val messageBody = smsMessage.messageBody.orEmpty()
                processSmsMessage(messageBody, sender, selectedBankConfigs)
            }
        }
    }

    private fun processSmsMessage(messageBody: String, sender: String?, bankConfigs: Set<BankSmsConfig>) {
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

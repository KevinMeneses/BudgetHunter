package com.meneses.budgethunter.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import com.meneses.budgethunter.commons.bank.SupportedBanks
import com.meneses.budgethunter.commons.data.PreferencesManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmsBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val smsService: SmsService by inject()
    private val preferencesManager: PreferencesManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        if (!preferencesManager.isSmsReadingEnabled) return

        val selectedBankIds = preferencesManager.selectedBankIds
        if (selectedBankIds.isEmpty()) {
            Log.w("SmsReceiver", "No hay bancos seleccionados.")
            return
        }

        val selectedBankConfigs = selectedBankIds.mapNotNull { bankId ->
            SupportedBanks.getBankConfigById(bankId)
        }.toSet()

        if (selectedBankConfigs.isEmpty()) {
            Log.w("SmsReceiver", "No se encontraron configuraciones válidas para los bancos seleccionados.")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEach { smsMessage ->
            val sender = smsMessage.originatingAddress
            val messageBody = smsMessage.messageBody.orEmpty()
            processSmsMessage(messageBody, sender, selectedBankConfigs)
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
        } else {
            Log.d("SmsReceiver", "SMS ignorado, no coincide con ningún banco seleccionado.")
        }
    }
}

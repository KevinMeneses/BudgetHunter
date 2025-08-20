package com.meneses.budgethunter.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.meneses.budgethunter.MyApplication
import com.meneses.budgethunter.commons.bank.SupportedBanks

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val preferencesManager = MyApplication.preferencesManager
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
            val messageBody = smsMessage.messageBody ?: ""

            // Check if the SMS is from any of the selected banks
            val isFromSelectedBank = sender != null && selectedBankConfigs.any { bankConfig ->
                bankConfig.senderKeywords.any { keyword ->
                    sender.contains(keyword, ignoreCase = true) || messageBody.contains(keyword, ignoreCase = true)
                }
            }

            if (isFromSelectedBank) {
                SmsService(context.applicationContext).processSms(messageBody, selectedBankConfigs)
            } else {
                Log.d("SmsReceiver", "SMS ignorado, no coincide con ningún banco seleccionado.")
            }
        }
    }
}

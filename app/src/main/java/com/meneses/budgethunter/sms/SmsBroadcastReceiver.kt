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

        val selectedBankId = preferencesManager.selectedBankId
        val bankConfig = SupportedBanks.getBankConfigById(selectedBankId)

        if (bankConfig == null) {
            Log.w("SmsReceiver", "No hay banco seleccionado o configuración no encontrada.")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEach { smsMessage ->
            val sender = smsMessage.originatingAddress
            val messageBody = smsMessage.messageBody ?: ""

            val isFromSelectedBank = sender != null && bankConfig.senderKeywords.any { keyword ->
                sender.contains(keyword, ignoreCase = true) || messageBody.contains(keyword, ignoreCase = true) // También revisa el cuerpo por si acaso
            }

            if (isFromSelectedBank) {
                SmsService(context).processSms(messageBody, bankConfig)
            } else {
                Log.d("SmsReceiver", "SMS ignorado, no coincide con remitente de ${bankConfig.displayName}.")
            }
        }
    }
} 

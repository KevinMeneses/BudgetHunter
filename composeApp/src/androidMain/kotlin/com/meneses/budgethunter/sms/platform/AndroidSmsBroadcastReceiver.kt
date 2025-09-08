package com.meneses.budgethunter.sms.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.SmsService
import com.meneses.budgethunter.sms.domain.SupportedBanks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/**
 * Android-specific BroadcastReceiver for SMS processing.
 * This component bridges Android SMS system with KMP SMS processing.
 */
class AndroidSmsBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val smsService: SmsService by inject()
    private val preferencesManager: PreferencesManager by inject()
    private val ioScope: CoroutineScope by inject(named("IOScope"))

    override fun onReceive(context: Context, intent: Intent) {
        ioScope.launch {
            try {
                if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                    Log.d("AndroidSmsBroadcast", "Received non-SMS intent: ${intent.action}")
                    return@launch
                }
                
                if (!preferencesManager.isSmsReadingEnabled()) {
                    Log.d("AndroidSmsBroadcast", "SMS reading is disabled")
                    return@launch
                }

                val selectedBankIds = preferencesManager.getSelectedBankIds()
                if (selectedBankIds.isEmpty()) {
                    Log.d("AndroidSmsBroadcast", "No banks selected for SMS processing")
                    return@launch
                }

                val selectedBankConfigs = selectedBankIds.mapNotNull { bankId ->
                    SupportedBanks.getBankConfigById(bankId)
                }.toSet()

                if (selectedBankConfigs.isEmpty()) {
                    Log.w("AndroidSmsBroadcast", "No valid bank configurations found")
                    return@launch
                }

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                messages?.forEach { smsMessage ->
                    val sender = smsMessage.originatingAddress
                    val messageBody = smsMessage.messageBody.orEmpty()
                    Log.d("AndroidSmsBroadcast", "Processing SMS from: $sender")
                    processSmsMessage(messageBody, sender, selectedBankConfigs)
                }
            } catch (e: Exception) {
                Log.e("AndroidSmsBroadcast", "Error processing SMS", e)
            }
        }
    }

    private suspend fun processSmsMessage(
        messageBody: String, 
        sender: String?, 
        bankConfigs: Set<BankSmsConfig>
    ) {
        val isFromSelectedBank = sender != null && bankConfigs.any { bankConfig ->
            bankConfig.senderKeywords.any { keyword ->
                sender.contains(keyword, ignoreCase = true) || messageBody.contains(keyword, ignoreCase = true)
            }
        }

        if (isFromSelectedBank) {
            Log.d("AndroidSmsBroadcast", "SMS matches selected bank, processing...")
            smsService.processSms(messageBody, bankConfigs)
        } else {
            Log.d("AndroidSmsBroadcast", "SMS does not match any selected bank")
        }
    }
}
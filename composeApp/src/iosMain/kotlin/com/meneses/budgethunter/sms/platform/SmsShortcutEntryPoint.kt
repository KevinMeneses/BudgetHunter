package com.meneses.budgethunter.sms.platform

import com.meneses.budgethunter.di.startKoinIfNeeded
import com.meneses.budgethunter.sms.application.RegisterSmsFromShortcutUseCase
import com.meneses.budgethunter.sms.application.SmsShortcutReply
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Called by the "Registrar gasto desde mensaje" App Intent (iosApp) with the text of the SMS that
 * a Shortcuts automation received. It works with the app closed: Koin is started here if no
 * screen did it first. The Swift side has to fill IOSBridge before calling it.
 */
class SmsShortcutEntryPoint : KoinComponent {

    init {
        startKoinIfNeeded()
    }

    private val registerSmsFromShortcut: RegisterSmsFromShortcutUseCase by inject()

    @Throws(CancellationException::class)
    suspend fun register(messageBody: String): SmsShortcutReply = registerSmsFromShortcut(messageBody)
}

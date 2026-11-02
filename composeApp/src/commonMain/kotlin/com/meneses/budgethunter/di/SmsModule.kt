package com.meneses.budgethunter.di

import com.meneses.budgethunter.sms.application.ProcessSmsUseCase
import com.meneses.budgethunter.sms.application.RegisterSmsEntryUseCase
import com.meneses.budgethunter.sms.application.RegisterSmsFromShortcutUseCase
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.SmsService
import org.koin.dsl.module

val smsModule = module {

    single {
        SmsMapper(
            preferencesManager = get(),
            budgetLocalDataSource = get(),
            stringResourceProvider = get()
        )
    }

    single {
        RegisterSmsEntryUseCase(
            smsMapper = get(),
            budgetEntryRepository = get()
        )
    }

    single<SmsService> {
        ProcessSmsUseCase(
            registerSmsEntry = get(),
            notificationManager = get(),
            stringResourceProvider = get()
        )
    }

    single {
        RegisterSmsFromShortcutUseCase(
            registerSmsEntry = get(),
            stringResourceProvider = get()
        )
    }
}

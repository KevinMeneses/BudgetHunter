package com.meneses.budgethunter.di

import com.meneses.budgethunter.sms.application.IsNotificationReaderStoppedUseCase
import com.meneses.budgethunter.sms.application.ProcessSmsUseCase
import com.meneses.budgethunter.sms.application.RegisterSmsEntryUseCase
import com.meneses.budgethunter.sms.application.RegisterSmsFromShortcutUseCase
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.BankNotificationService
import com.meneses.budgethunter.sms.domain.SmsService
import org.koin.dsl.module

val smsModule = module {

    factory {
        IsNotificationReaderStoppedUseCase(
            preferencesManager = get(),
            permissionsManager = get()
        )
    }

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

    single {
        ProcessSmsUseCase(
            registerSmsEntry = get(),
            notificationManager = get(),
            stringResourceProvider = get()
        )
    }

    single<SmsService> { get<ProcessSmsUseCase>() }

    single<BankNotificationService> { get<ProcessSmsUseCase>() }

    single {
        RegisterSmsFromShortcutUseCase(
            registerSmsEntry = get(),
            stringResourceProvider = get()
        )
    }
}

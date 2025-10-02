package com.meneses.budgethunter.di

import com.meneses.budgethunter.sms.application.ProcessSmsUseCase
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.SmsService
import org.koin.dsl.module

val smsModule = module {
    
    single { SmsMapper(preferencesManager = get()) }
    
    single<SmsService> {
        ProcessSmsUseCase(
            smsMapper = get(),
            budgetEntryRepository = get(),
            notificationManager = get()
        )
    }
}

package com.meneses.budgethunter.sms.di

import android.content.Context
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.sms.SmsMapper
import com.meneses.budgethunter.sms.SmsService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class SmsModule {

    @Single
    fun provideSmsMapper(
        preferencesManager: PreferencesManager
    ): SmsMapper = SmsMapper(preferencesManager)

    @Single
    fun provideSmsService(
        context: Context,
        smsMapper: SmsMapper,
        budgetEntryRepository: BudgetEntryRepository,
        @Named("IO") ioDispatcher: CoroutineDispatcher
    ): SmsService = SmsService(
        context = context,
        smsMapper = smsMapper,
        budgetEntryRepository = budgetEntryRepository,
        scope = CoroutineScope(ioDispatcher)
    )
}

package com.meneses.budgethunter.sms.di

import android.content.Context
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.AndroidNotificationService
import com.meneses.budgethunter.commons.platform.NotificationService
import com.meneses.budgethunter.sms.SmsService
import com.meneses.budgethunter.sms.SmsMapper
import com.meneses.budgethunter.sms.ProcessSmsUseCase
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
    fun provideNotificationService(context: Context): NotificationService {
        return AndroidNotificationService(context)
    }

    @Single
    fun provideSmsService(
        context: Context,
        smsMapper: SmsMapper,
        budgetEntryRepository: BudgetEntryRepository,
        notificationService: NotificationService,
        @Named("IO") ioDispatcher: CoroutineDispatcher
    ): SmsService = ProcessSmsUseCase(
        context = context,
        smsMapper = smsMapper,
        budgetEntryRepository = budgetEntryRepository,
        notificationService = notificationService,
        scope = CoroutineScope(ioDispatcher)
    )
}

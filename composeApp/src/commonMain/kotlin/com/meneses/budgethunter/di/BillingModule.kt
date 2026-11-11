package com.meneses.budgethunter.di

import com.meneses.budgethunter.billing.data.BillingApiService
import com.meneses.budgethunter.billing.data.PremiumRepository
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Premium purchases. The platform module provides the [com.meneses.budgethunter.billing.domain.StoreBilling]. */
val billingModule = module {
    single<BillingApiService> {
        BillingApiService(httpClient = get<HttpClient>(named("AuthHttpClient")), json = get())
    }

    single<PremiumRepository> {
        PremiumRepository(billingApi = get(), store = get(), logger = get())
    }
}

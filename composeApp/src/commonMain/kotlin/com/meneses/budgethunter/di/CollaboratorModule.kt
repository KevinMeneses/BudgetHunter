package com.meneses.budgethunter.di

import com.meneses.budgethunter.collaborator.CollaboratorsViewModel
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import com.meneses.budgethunter.collaborator.data.network.CollaboratorApiService
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

val collaboratorModule = module {

    single<CollaboratorApiService> {
        CollaboratorApiService(
            httpClient = get<HttpClient>(named("AuthHttpClient")),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    single<CollaboratorRepository> {
        CollaboratorRepository(
            collaboratorApiService = get(),
            ioDispatcher = get<CoroutineDispatcher>(named("IO"))
        )
    }

    factory { (budgetServerId: Long) ->
        CollaboratorsViewModel(
            collaboratorRepository = get(),
            budgetServerId = budgetServerId
        )
    }
}

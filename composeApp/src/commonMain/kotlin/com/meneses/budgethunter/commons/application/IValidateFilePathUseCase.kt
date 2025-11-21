package com.meneses.budgethunter.commons.application

interface IValidateFilePathUseCase {
    suspend fun execute(filePath: String?): String?
}

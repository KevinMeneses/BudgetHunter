package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.commons.application.ValidateFilePathUseCase

class FakeValidateFilePathUseCase : ValidateFilePathUseCase(null!!) {
    var validPath: String? = "/valid/path.pdf"

    override suspend fun execute(filePath: String): String? = validPath
}

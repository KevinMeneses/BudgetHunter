package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.commons.application.IValidateFilePathUseCase

class FakeValidateFilePathUseCase : IValidateFilePathUseCase {
    var validPath: String? = "/valid/path.pdf"

    override suspend fun execute(filePath: String): String? = validPath
}

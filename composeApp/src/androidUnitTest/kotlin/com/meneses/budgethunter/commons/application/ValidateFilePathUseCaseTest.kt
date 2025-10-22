package com.meneses.budgethunter.commons.application

import com.meneses.budgethunter.commons.data.FileManager
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidateFilePathUseCaseTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory(prefix = "budgethunter-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `returns same path when file exists`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val file = File(tempDir, "invoice.png").apply { writeText("stub") }
        val useCase = ValidateFilePathUseCase(FileManager(), dispatcher)

        val result = useCase.execute(file.absolutePath)

        assertEquals(file.absolutePath, result)
    }

    @Test
    fun `returns null when file does not exist or path is blank`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val useCase = ValidateFilePathUseCase(FileManager(), dispatcher)

        val missingResult = useCase.execute(File(tempDir, "missing.pdf").absolutePath)
        val nullResult = useCase.execute(null)
        val blankResult = useCase.execute(" ")

        assertNull(missingResult)
        assertNull(nullResult)
        assertNull(blankResult)
    }
}

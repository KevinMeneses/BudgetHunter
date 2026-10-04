package com.meneses.budgethunter.budgetEntry.data.remote

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.sync.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

/**
 * Remote data source for Gemini API.
 * Handles HTTP requests to Google's Gemini REST API for AI-powered budget entry extraction.
 */
class GeminiApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val json: Json,
    private val logger: Logger
) {
    private val tag = "GeminiApiClient"

    /**
     * Sends an image and prompt to Gemini API and returns the extracted budget entry.
     *
     * @param base64Image Base64-encoded JPEG image
     * @param prompt The extraction prompt
     * @return Extracted BudgetEntry or null if parsing fails
     */
    suspend fun extractBudgetEntryFromImage(
        base64Image: String,
        prompt: String
    ): BudgetEntry? {
        return try {
            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(
                                inlineData = InlineData(
                                    mimeType = "image/jpeg",
                                    data = base64Image
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = EXTRACTION_SCHEMA,
                    temperature = 0.1
                )
            )

            val response = httpClient.post(ENDPOINT) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val geminiResponse = response.body<GeminiResponse>()
            val responseText = geminiResponse.candidates?.firstOrNull()?.content?.parts
                ?.firstOrNull { it.text != null }?.text
                ?: return null

            parseExtraction(responseText)
        } catch (e: Exception) {
            logger.error(tag, "Gemini API error", e)
            null
        }
    }

    private fun parseExtraction(responseText: String): BudgetEntry? {
        // Structured output should already be plain JSON; strip code fences defensively
        val refinedText = responseText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        if (refinedText.isBlank() || refinedText == "{}") return null

        val extraction = json.decodeFromString<GeminiExtraction>(refinedText)
        if (!extraction.isInvoice) return null

        val amount = extraction.amount?.content?.trim().orEmpty()
        val description = extraction.description?.trim().orEmpty()
        if (amount.isEmpty() && description.isEmpty()) return null

        val category = BudgetEntry.Category.entries
            .firstOrNull { it.name.equals(extraction.category?.trim(), ignoreCase = true) }
            ?: BudgetEntry.Category.OTHER

        val defaults = BudgetEntry()
        return defaults.copy(
            amount = amount,
            description = description,
            category = category,
            date = extraction.date?.takeIf { ISO_DATE.matches(it.trim()) }?.trim() ?: defaults.date
        )
    }

    private companion object {
        const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")

        val EXTRACTION_SCHEMA = GeminiSchema(
            type = "OBJECT",
            properties = mapOf(
                "isInvoice" to GeminiSchema(
                    type = "BOOLEAN",
                    description = "true only if the image is a receipt, invoice or bill"
                ),
                "amount" to GeminiSchema(
                    type = "NUMBER",
                    description = "Final total paid as a plain number: dot as decimal separator, no thousands separators, no currency symbol"
                ),
                "description" to GeminiSchema(
                    type = "STRING",
                    description = "Short description of what was paid"
                ),
                "category" to GeminiSchema(
                    type = "STRING",
                    enum = BudgetEntry.Category.entries.map { it.name }
                ),
                "date" to GeminiSchema(
                    type = "STRING",
                    description = "Date of the purchase in YYYY-MM-DD format"
                )
            ),
            required = listOf("isInvoice")
        )
    }
}

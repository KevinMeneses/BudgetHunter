package com.meneses.budgethunter.budgetEntry.data.remote

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
    private val json: Json
) {
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
            // Construct Gemini API request
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
                )
            )

            // Call Gemini API
            val response = httpClient.post("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(GeminiRequest.serializer(), requestBody))
            }

            // Parse response
            val geminiResponse = json.decodeFromString<GeminiResponse>(response.body())
            val responseText = geminiResponse.candidates?.firstOrNull()?.content?.parts
                ?.firstOrNull { it.text != null }?.text
                ?: return null

            // Clean up the response text (remove markdown code blocks)
            val refinedText = responseText
                .removePrefix("```json\n")
                .removeSuffix("```")
                .trim()

            // Parse the JSON response to BudgetEntry
            if (refinedText.isNotBlank() && refinedText != "{}") {
                json.decodeFromString<BudgetEntry>(refinedText)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Gemini API Error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

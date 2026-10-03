package com.meneses.budgethunter.budgetEntry.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * Gemini API request/response data models.
 * These DTOs represent the structure of Google's Gemini REST API.
 */

@Serializable
internal data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
internal data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: GeminiSchema? = null,
    val temperature: Double? = null
)

/** Subset of the Gemini (OpenAPI-style) response schema needed to force structured output. */
@Serializable
internal data class GeminiSchema(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null,
    val properties: Map<String, GeminiSchema>? = null,
    val required: List<String>? = null
)

/**
 * Lenient shape of the model's JSON answer. Kept separate from BudgetEntry so that an unexpected
 * value (unknown category, numeric amount, missing field) doesn't discard the whole response.
 */
@Serializable
internal data class GeminiExtraction(
    val isInvoice: Boolean = true,
    val amount: JsonPrimitive? = null,
    val description: String? = null,
    val category: String? = null,
    val date: String? = null
)

@Serializable
internal data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
internal data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
internal data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null
)

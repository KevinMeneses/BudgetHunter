package com.meneses.budgethunter.budgetEntry.data.remote

import kotlinx.serialization.Serializable

/**
 * Gemini API request/response data models.
 * These DTOs represent the structure of Google's Gemini REST API.
 */

@Serializable
internal data class GeminiRequest(
    val contents: List<GeminiContent>
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

package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** Body of `POST /api/receipts/extract`. */
@Serializable
data class ReceiptExtractionRequest(
    /** The file, Base64-encoded. */
    val data: String,
    val mimeType: String,
    /** The user's local date (YYYY-MM-DD), used when the document shows none. */
    val today: String
)

/** What the server's AI read from the document, plus the month's usage after this scan. */
@Serializable
data class ReceiptExtractionResponse(
    val isReceipt: Boolean,
    // A JSON number on the wire; read as a primitive so no precision is lost on the way to the form's text field.
    val amount: JsonPrimitive? = null,
    val description: String? = null,
    val category: String? = null,
    val date: String? = null,
    val usage: AiUsageResponse? = null
)

/** Body of an error answer from the receipt endpoints. */
@Serializable
data class ReceiptErrorResponse(
    val status: Int = 0,
    val code: String = "",
    val message: String = "",
    val usage: AiUsageResponse? = null
)

/** How much of the paid AI the account used this month (`GET /api/receipts/usage`). */
@Serializable
data class AiUsageResponse(
    val plan: String,
    val period: String,
    val receiptScansUsed: Int,
    val receiptScansLimit: Int,
    val receiptScansRemaining: Int,
    val resetsOn: String
)

package com.photocleaner.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenAIRequest(
    @Json(name = "model") val model: String = "mimo-v2.5",
    @Json(name = "messages") val messages: List<Message>,
    @Json(name = "max_tokens") val maxTokens: Int = 4000
)

@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: MessageContent
)

sealed class MessageContent {
    data class TextContent(val text: String) : MessageContent()
    data class ImageListContent(val images: List<ImageContent>) : MessageContent()
}

@JsonClass(generateAdapter = true)
data class ImageContent(
    @Json(name = "type") val type: String,
    @Json(name = "text") val text: String? = null,
    @Json(name = "image_url") val imageUrl: ImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class ImageUrl(
    @Json(name = "url") val url: String,
    @Json(name = "detail") val detail: String = "low"
)

@JsonClass(generateAdapter = true)
data class OpenAIResponse(
    @Json(name = "choices") val choices: List<Choice>
)

@JsonClass(generateAdapter = true)
data class Choice(
    @Json(name = "message") val message: ResponseMessage
)

@JsonClass(generateAdapter = true)
data class ResponseMessage(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ClassificationResult(
    @Json(name = "classification") val classification: String,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "category") val category: String,
    @Json(name = "reason") val reason: String = ""
)

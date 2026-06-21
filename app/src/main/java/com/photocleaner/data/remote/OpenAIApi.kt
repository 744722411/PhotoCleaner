package com.photocleaner.data.remote

import com.photocleaner.data.remote.dto.OpenAIRequest
import com.photocleaner.data.remote.dto.OpenAIResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun classifyImage(
        @Body request: OpenAIRequest
    ): OpenAIResponse
}

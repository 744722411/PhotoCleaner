package com.photocleaner.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.photocleaner.data.remote.dto.*
import com.photocleaner.domain.model.Classification
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

@Singleton
class OpenAIService @Inject constructor(
    private val api: OpenAIApi,
    private val moshi: Moshi,
    @ApplicationContext private val context: Context
) {
    suspend fun classifyPhoto(uri: String, apiKey: String, model: String = "mimo-v2.5"): Triple<Classification, Float, String> =
        withContext(Dispatchers.IO) {
            val base64 = encodeImageToBase64(Uri.parse(uri))

            val prompt = """你是一个照片清理助手。分析这张照片，判断是否应该删除。返回JSON格式:
{"classification":"useless/keep/uncertain","confidence":0.0-1.0,"category":"具体类别","reason":"原因"}

安全第一原则：宁可漏掉也不要误删！只有你非常确定（confidence >= 0.9）这张照片是废片时，才标为useless。如果有一丝犹豫，标为uncertain或keep。

判定为useless（高置信度建议删除）的照片类型：
- blank: 完全空白、纯色照片、全黑全白全灰、确实没有任何内容
- blurry: 严重模糊/抖动，完全看不清拍摄对象
- accidental: 明确的口袋误拍、遮挡镜头、极端过曝/过暗

判定为uncertain（需要用户自己判断）的照片类型：
- screenshot: 手机截图（可能是用户有意保留的）
- chat_screenshot: 聊天对话截图
- receipt: 收据、发票、快递单（可能需要留作凭证）
- qrcode: 二维码、条形码照片
- document_temp: 文档截图、备忘录截图
- error_screen: 错误提示截图
- ad: 广告弹窗截图
- temporary: 临时保存的照片

判定为keep（建议保留）的照片类型：
- personal: 个人照片、生活照
- landscape: 风景、旅行、建筑
- people: 人物照片、合照
- pet: 宠物照片
- food: 美食照片
- selfie: 自拍
- art: 摄影作品、艺术创作
- memorable: 有纪念意义的照片

注意：截图、收据、二维码等虽然可能是临时的，但也可能是用户有意保存的。不能假设它们无用。只有完全空白、严重模糊、明确误拍的照片才能确定是废片。
重要：只返回一个JSON对象，不要任何其他文字、解释、markdown格式或代码块标记。直接输出如下格式的JSON：
{"classification":"useless/keep/uncertain","confidence":0.0-1.0,"category":"类别","reason":"原因"}
不要用```包裹，不要添加任何前缀或后缀文字。"""

            val request = OpenAIRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            ImageContent(type = "text", text = prompt),
                            ImageContent(
                                type = "image_url",
                                imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64", detail = "low")
                            )
                        )
                    )
                )
            )

            val response = api.classifyImage(request)
            val content = response.choices.firstOrNull()?.message?.content
                ?: return@withContext Triple(Classification.UNKNOWN, 0f, "parse_error")

            parseClassification(content)
        }

    suspend fun testConnection(baseUrl: String, apiKey: String, model: String = "mimo-v2.5"): String =
        withContext(Dispatchers.IO) {
            val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val tempRetrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("Authorization", "Bearer $apiKey")
                            .build()
                        chain.proceed(request)
                    }
                    .build())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            val tempApi = tempRetrofit.create(OpenAIApi::class.java)

            val request = OpenAIRequest(
                model = model,
                messages = listOf(
                    Message(role = "user", content = "hello")
                ),
                maxTokens = 10
            )
            val response = tempApi.classifyImage(request)
            val content = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("No response from API")
            content
        }

    private fun encodeImageToBase64(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap == null) return ""

        var scaled: Bitmap? = null
        try {
            val maxDim = 512
            scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else bitmap

            val outputStream = ByteArrayOutputStream()
            (scaled).compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } finally {
            // Recycle scaled bitmap only if it's a different instance from the original
            if (scaled !== bitmap) {
                scaled?.recycle()
            }
            bitmap.recycle()
        }
    }

    private fun parseClassification(content: String): Triple<Classification, Float, String> {
        val trimmed = content.trim()
        Log.d("OpenAIService", "AI raw response: $trimmed")

        // Step 1: Try to extract and parse JSON directly
        val result = tryParseJson(trimmed)
        if (result != null) {
            return buildTriple(result)
        }

        // Step 2: Strip markdown code fences (```json ... ``` or ``` ... ```)
        val stripped = trimmed
            .replace(Regex("^```(?:json|JSON)?\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^```\\s*$", RegexOption.MULTILINE), "")
            .trim()
        if (stripped != trimmed) {
            val result2 = tryParseJson(stripped)
            if (result2 != null) {
                return buildTriple(result2)
            }
        }

        // Step 3: Try to find JSON object within surrounding text
        val jsonMatch = Regex("\\{[^{}]*\"(?:classification|Classification)\"[^{}]*\\}").find(trimmed)
        if (jsonMatch != null) {
            val result3 = tryParseJson(jsonMatch.value)
            if (result3 != null) {
                return buildTriple(result3)
            }
        }

        // Step 4: Fallback - try to extract classification from plain text
        val plainResult = tryExtractFromPlainText(trimmed)
        if (plainResult != null) {
            return plainResult
        }

        Log.w("OpenAIService", "Failed to parse AI response: $trimmed")
        return Triple(Classification.UNKNOWN, 0f, "parse_error")
    }

    private fun tryParseJson(jsonStr: String): ClassificationResult? {
        return try {
            // Normalize: handle case-insensitive keys by replacing them
            val normalized = jsonStr
                .replace(Regex("\"Classification\"", RegexOption.IGNORE_CASE), "\"classification\"")
                .replace(Regex("\"Confidence\"", RegexOption.IGNORE_CASE), "\"confidence\"")
                .replace(Regex("\"Category\"", RegexOption.IGNORE_CASE), "\"category\"")
                .replace(Regex("\"Reason\"", RegexOption.IGNORE_CASE), "\"reason\"")
            val adapter = moshi.adapter(ClassificationResult::class.java)
            adapter.fromJson(normalized)
        } catch (e: Exception) {
            Log.d("OpenAIService", "JSON parse attempt failed: ${e.message}")
            null
        }
    }

    private fun buildTriple(result: ClassificationResult): Triple<Classification, Float, String> {
        val classification = when (result.classification.lowercase().trim()) {
            "useless" -> Classification.USELESS
            "keep" -> Classification.KEEP
            "uncertain" -> Classification.UNCERTAIN
            else -> Classification.UNKNOWN
        }
        return Triple(classification, result.confidence.coerceIn(0f, 1f), result.category)
    }

    private fun tryExtractFromPlainText(text: String): Triple<Classification, Float, String>? {
        val lower = text.lowercase()
        val classification = when {
            lower.contains("useless") -> Classification.USELESS
            lower.contains("keep") -> Classification.KEEP
            lower.contains("uncertain") -> Classification.UNCERTAIN
            else -> return null
        }
        // Try to extract confidence number
        val confidenceMatch = Regex("0?\\.\\d+").find(text)
        val confidence = confidenceMatch?.value?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
        // Try to extract category
        val categoryMatch = Regex("\"category\"\\s*[:：]\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(text)
        val category = categoryMatch?.groupValues?.get(1) ?: "unknown"
        Log.d("OpenAIService", "Extracted from plain text: $classification, $confidence, $category")
        return Triple(classification, confidence, category)
    }
}

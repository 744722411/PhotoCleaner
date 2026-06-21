package com.photocleaner.domain.usecase

import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class ClassifyLog(
    val photoName: String,
    val status: ClassifyLogStatus,
    val message: String,
    val aiResult: String = ""
)

enum class ClassifyLogStatus { PROCESSING, SUCCESS, SKIP, ERROR, INFO }

class ClassifyPhotosUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    companion object {
        private const val MAX_CONCURRENT = 3
    }

    suspend operator fun invoke(
        photos: List<Photo>,
        isPaused: () -> Boolean = { false },
        onProgress: (classified: Int, total: Int) -> Unit = { _, _ -> },
        onLog: (ClassifyLog) -> Unit = {}
    ): List<Photo> = withContext(Dispatchers.IO) {
        val unclassified = photos.filter { it.classification == Classification.UNKNOWN || it.classification == Classification.UNCERTAIN }
        if (unclassified.isEmpty()) {
            onLog(ClassifyLog("", ClassifyLogStatus.INFO, "没有需要AI分类的照片"))
            return@withContext emptyList()
        }

        onLog(ClassifyLog("", ClassifyLogStatus.INFO, "开始AI分类 ${unclassified.size} 张照片..."))

        val completedCount = AtomicInteger(0)

        // Process sequentially with pause support (easier to log and pause)
        val results = mutableListOf<Photo>()
        for (photo in unclassified) {
            // Check for pause
            while (isPaused()) {
                delay(200)
                coroutineContext.ensureActive()
            }
            coroutineContext.ensureActive()

            onLog(ClassifyLog(photo.displayName, ClassifyLogStatus.PROCESSING, "正在调用AI分析..."))

            try {
                val classified = repository.classifyPhoto(photo)
                repository.updateClassification(
                    classified.id,
                    classified.classification,
                    classified.confidence,
                    classified.category
                )

                val classificationText = when (classified.classification) {
                    Classification.USELESS -> "无用"
                    Classification.KEEP -> "保留"
                    Classification.UNCERTAIN -> "待定"
                    Classification.UNKNOWN -> "未知"
                }

                val categoryDisplay = classified.category.ifBlank { "无" }
                val confidencePercent = (classified.confidence * 100).toInt()

                val resultText = "$classificationText - $categoryDisplay ($confidencePercent%)"

                if (classified.classification == Classification.USELESS) {
                    onLog(ClassifyLog(
                        photo.displayName,
                        ClassifyLogStatus.SUCCESS,
                        "🗑️ $resultText",
                        aiResult = resultText
                    ))
                } else {
                    onLog(ClassifyLog(
                        photo.displayName,
                        ClassifyLogStatus.SUCCESS,
                        "✅ $resultText",
                        aiResult = resultText
                    ))
                }

                results.add(classified)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onLog(ClassifyLog(
                    photo.displayName,
                    ClassifyLogStatus.ERROR,
                    "❌ AI分析失败: ${e.message}"
                ))
                results.add(photo)
            }

            val count = completedCount.incrementAndGet()
            onProgress(count, unclassified.size)
        }

        onLog(ClassifyLog("", ClassifyLogStatus.INFO, "AI分类完成！"))
        results
    }
}

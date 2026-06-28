package com.photocleaner.domain.usecase

import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.service.PhotoClassifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class ScanLog(
    val photoName: String,
    val status: ScanLogStatus,
    val message: String
)

enum class ScanLogStatus { PROCESSING, LOCAL_HIT, SUCCESS, ERROR, INFO }

class ScanPhotosUseCase @Inject constructor(
    private val repository: PhotoRepository,
    private val classifier: PhotoClassifier
) {
    suspend operator fun invoke(
        selectedDirectories: Set<String> = emptySet(),
        batchSize: Int = 0,
        isPaused: () -> Boolean,
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> },
        onLog: (ScanLog) -> Unit = {}
    ): List<Photo> {
        // Repositories are main-safe (they switch to IO internally), so this use case
        // does not wrap the whole flow in withContext — avoiding double dispatching.
        onLog(ScanLog("", ScanLogStatus.INFO, "正在扫描相册..."))
        val existingPhotoIds = repository.getAllPhotoIds().toSet()
        val photos = if (selectedDirectories.isNotEmpty()) {
            repository.scanPhotos(selectedDirectories)
        } else {
            repository.scanPhotos()
        }
        val scannedIds = photos.map { it.id }.toSet()
        val toDelete = existingPhotoIds - scannedIds
        if (toDelete.isNotEmpty()) {
            onLog(ScanLog("", ScanLogStatus.INFO, "清理 ${toDelete.size} 张已删除照片的记录..."))
            repository.deletePhotosByIds(toDelete.toList())
        }

        val newPhotos = photos.filter { it.id !in existingPhotoIds }

        val limited = if (batchSize > 0 && newPhotos.size > batchSize) {
            onLog(ScanLog("", ScanLogStatus.INFO, "共发现 ${newPhotos.size} 张新照片，本次处理前 $batchSize 张"))
            newPhotos.take(batchSize)
        } else {
            newPhotos
        }
        val total = limited.size
        if (total == 0) {
            onLog(ScanLog("", ScanLogStatus.INFO, "没有发现新照片，扫描完成！"))
            return emptyList()
        }
        onLog(ScanLog("", ScanLogStatus.INFO, "发现 $total 张新照片，开始本地检测..."))

        val results = mutableListOf<Photo>()
        val pendingInsert = mutableListOf<Photo>()
        val completed = AtomicInteger(0)
        val classifyDispatcher = Dispatchers.IO.limitedParallelism(CLASSIFY_PARALLELISM)

        limited.chunked(CLASSIFY_PARALLELISM).forEach { chunk ->
            while (isPaused()) {
                delay(200)
                coroutineContext.ensureActive()
            }
            coroutineContext.ensureActive()

            chunk.forEach { onLog(ScanLog(it.displayName, ScanLogStatus.PROCESSING, "正在本地检测...")) }

            val chunkResults = coroutineScope {
                chunk.map { photo ->
                    async(classifyDispatcher) {
                        try {
                            classifier.classify(photo)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            onLog(ScanLog(photo.displayName, ScanLogStatus.ERROR, "检测失败: ${e.message}"))
                            photo
                        }
                    }
                }.awaitAll()
            }

            chunkResults.forEach { result ->
                results.add(result)
                pendingInsert.add(result)
                if (pendingInsert.size >= INSERT_BATCH) {
                    repository.insertPhotos(pendingInsert.toList())
                    pendingInsert.clear()
                }
                val done = completed.incrementAndGet()
                onProgress(done, total)
                if (result.isLocalUseless) {
                    onLog(ScanLog(
                        result.displayName,
                        ScanLogStatus.LOCAL_HIT,
                        "发现问题: ${result.localReason} → ${result.classification.displayName} (${(result.confidence * 100).toInt()}%)"
                    ))
                } else {
                    onLog(ScanLog(result.displayName, ScanLogStatus.SUCCESS, "本地检测通过"))
                }
            }
        }

        if (pendingInsert.isNotEmpty()) {
            repository.insertPhotos(pendingInsert.toList())
            pendingInsert.clear()
        }

        onLog(ScanLog("", ScanLogStatus.INFO, "扫描完成！共 ${results.size} 张新照片已录入"))
        return results
    }

    private companion object {
        const val CLASSIFY_PARALLELISM = 4
        const val INSERT_BATCH = 20
    }
}

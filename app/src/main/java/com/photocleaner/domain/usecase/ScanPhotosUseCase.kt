package com.photocleaner.domain.usecase

import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.service.PhotoClassifier
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

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
        rescanExistingPhotos: Boolean = false,
        isPaused: () -> Boolean,
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> },
        onLog: (ScanLog) -> Unit = {}
    ): List<Photo> {
        onLog(ScanLog("", ScanLogStatus.INFO, "正在扫描相册..."))
        val existingPhotoIds = repository.getAllPhotoIds().toSet()
        val photos = if (selectedDirectories.isNotEmpty()) {
            repository.scanPhotos(selectedDirectories)
        } else {
            repository.scanPhotos()
        }

        if (selectedDirectories.isEmpty()) {
            val scannedIds = photos.map { it.id }.toSet()
            val toDelete = existingPhotoIds - scannedIds
            if (toDelete.isNotEmpty()) {
                onLog(ScanLog("", ScanLogStatus.INFO, "清理 ${toDelete.size} 张已删除照片的记录..."))
                repository.deletePhotosByIds(toDelete.toList())
            }
        }

        val candidates = if (rescanExistingPhotos) {
            photos
        } else {
            photos.filter { it.id !in existingPhotoIds }
        }
        val modeLabel = if (rescanExistingPhotos) "照片" else "新照片"
        val limited = if (batchSize > 0 && candidates.size > batchSize) {
            onLog(ScanLog("", ScanLogStatus.INFO, "共发现 ${candidates.size} 张$modeLabel，本次处理前 $batchSize 张"))
            candidates.take(batchSize)
        } else {
            candidates
        }

        val total = limited.size
        if (total == 0) {
            val message = if (rescanExistingPhotos) {
                "当前目录没有可检测照片，扫描完成！"
            } else {
                "没有发现新照片，扫描完成！如需更新已入库照片，请在设置中打开“重新检测已入库照片”。"
            }
            onLog(ScanLog("", ScanLogStatus.INFO, message))
            return emptyList()
        }
        val startMessage = if (rescanExistingPhotos) {
            "将重新检测 $total 张照片..."
        } else {
            "发现 $total 张新照片，开始本地检测..."
        }
        onLog(ScanLog("", ScanLogStatus.INFO, startMessage))

        val results = mutableListOf<Photo>()
        val pendingInsert = mutableListOf<Photo>()
        val completed = AtomicInteger(0)

        limited.chunked(CLASSIFY_PARALLELISM).forEach { chunk ->
            while (isPaused()) {
                delay(200)
                coroutineContext.ensureActive()
            }
            coroutineContext.ensureActive()

            val chunkResults = coroutineScope {
                chunk.map { photo ->
                    async(Dispatchers.IO) {
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
                val existing = if (rescanExistingPhotos) repository.getPhotoById(result.id) else null
                val storedResult = if (existing != null) {
                    result.copy(isInTrash = existing.isInTrash)
                } else {
                    result
                }
                results.add(storedResult)
                pendingInsert.add(storedResult)
                val done = completed.incrementAndGet()
                onProgress(done, total)
                if (storedResult.isLocalUseless) {
                    onLog(
                        ScanLog(
                            storedResult.displayName,
                            ScanLogStatus.LOCAL_HIT,
                            "发现问题: ${storedResult.localReason} -> ${storedResult.classification.displayName} (${(storedResult.confidence * 100).toInt()}%)"
                        )
                    )
                }
            }

            if (pendingInsert.isNotEmpty()) {
                repository.insertPhotos(pendingInsert.toList())
                pendingInsert.clear()
            }
        }

        onLog(ScanLog("", ScanLogStatus.INFO, "扫描完成！共 ${results.size} 张${modeLabel}已录入"))
        return results
    }

    private companion object {
        const val CLASSIFY_PARALLELISM = 1
    }
}

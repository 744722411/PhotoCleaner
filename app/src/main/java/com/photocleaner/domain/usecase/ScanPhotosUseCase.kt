package com.photocleaner.domain.usecase

import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class ScanLog(
    val photoName: String,
    val status: ScanLogStatus,
    val message: String
)

enum class ScanLogStatus { PROCESSING, LOCAL_HIT, SUCCESS, ERROR, INFO }

class ScanPhotosUseCase @Inject constructor(
    private val repository: PhotoRepository
) {
    suspend operator fun invoke(
        selectedDirectories: Set<String> = emptySet(),
        batchSize: Int = 0,
        isPaused: () -> Boolean,
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> },
        onLog: (ScanLog) -> Unit = {}
    ): List<Photo> = withContext(Dispatchers.IO) {
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
            return@withContext emptyList()
        }
        onLog(ScanLog("", ScanLogStatus.INFO, "发现 $total 张新照片，开始本地检测..."))

        val results = mutableListOf<Photo>()

        limited.forEachIndexed { index, photo ->
            // Check for pause
            while (isPaused()) {
                delay(200)
                coroutineContext.ensureActive()
            }
            coroutineContext.ensureActive()

            onLog(ScanLog(photo.displayName, ScanLogStatus.PROCESSING, "正在本地检测..."))
            onProgress(index + 1, total)

            try {
                val localResult = repository.detectLocalIssues(photo)
                results.add(localResult)

                if (localResult.isLocalUseless) {
                    onLog(ScanLog(
                        photo.displayName,
                        ScanLogStatus.LOCAL_HIT,
                        "发现问题: ${localResult.localReason} → ${localResult.classification.displayName} (${(localResult.confidence * 100).toInt()}%)"
                    ))
                } else {
                    onLog(ScanLog(photo.displayName, ScanLogStatus.SUCCESS, "本地检测通过"))
                }
            } catch (e: Exception) {
                onLog(ScanLog(photo.displayName, ScanLogStatus.ERROR, "检测失败: ${e.message}"))
                results.add(photo)
            }
        }

        onLog(ScanLog("", ScanLogStatus.INFO, "本地检测完成，正在保存结果..."))
        repository.insertPhotos(results)
        onLog(ScanLog("", ScanLogStatus.INFO, "扫描完成！共 ${results.size} 张照片"))
        results
    }
}

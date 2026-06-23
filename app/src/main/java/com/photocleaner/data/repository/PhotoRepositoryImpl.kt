package com.photocleaner.data.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.photocleaner.data.local.PhotoDao
import com.photocleaner.data.local.entity.PhotoEntity
import com.photocleaner.domain.model.DirectoryInfo
import com.photocleaner.data.remote.OpenAIService
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.util.BlurDetector
import com.photocleaner.util.ScreenshotDetector
import com.photocleaner.util.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    private val openAIService: OpenAIService,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : PhotoRepository {

    override fun getAllPhotos(): Flow<List<Photo>> =
        photoDao.getAllPhotos().map { entities -> entities.map { it.toDomain() } }

    override fun getPhotosByClassification(classification: Classification): Flow<List<Photo>> =
        photoDao.getPhotosByClassification(classification.name).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getClassifiedCount(): Flow<Int> = photoDao.getClassifiedCount()
    override fun getTotalCount(): Flow<Int> = photoDao.getTotalCount()
    override fun getUselessCount(): Flow<Int> = photoDao.getUselessCount()
    override fun getUselessSize(): Flow<Long> = photoDao.getUselessSize()

    override suspend fun scanPhotos(): List<Photo> = scanPhotos(emptySet())

    override suspend fun scanPhotos(selectedDirectories: Set<String>): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATA
        )

        val useDirectoryFilter = selectedDirectories.isNotEmpty()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val relPathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                // Directory filtering
                if (useDirectoryFilter) {
                    val relativePath = cursor.getString(relPathCol) ?: ""
                    val fullPath = cursor.getString(dataCol) ?: ""
                    val matchesAny = selectedDirectories.any { dir ->
                        relativePath.startsWith(dir, ignoreCase = true) ||
                        fullPath.contains("/$dir/", ignoreCase = true) ||
                        fullPath.contains("\\$dir\\", ignoreCase = true)
                    }
                    if (!matchesAny) continue
                }

                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                photos.add(
                    Photo(
                        id = id,
                        uri = uri,
                        displayName = cursor.getString(nameCol) ?: "unknown",
                        mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        size = cursor.getLong(sizeCol),
                        dateAdded = cursor.getLong(dateAddedCol),
                        dateModified = cursor.getLong(dateModCol),
                        filePath = cursor.getString(dataCol) ?: ""
                    )
                )
            }
        }

        photos
    }

    companion object {
        /** Minimum number of qualifying images for a directory to appear */
        private const val MIN_IMAGE_COUNT = 3
        /** Minimum file size in bytes (100 KB) */
        private const val MIN_FILE_SIZE = 100 * 1024L
        /** Minimum image width and height in pixels */
        private const val MIN_DIMENSION = 200

        /** Path segments (case-insensitive) that indicate asset / non-user directories */
        private val EXCLUDED_DIR_PATTERNS = listOf(
            "drawable", "assets", "res", "mipmap",
            "emoji", "sticker", "emoticon",
            "wallpaper", "background", "launcher",
            "cache", "temp", "tmp", "thumb",
            "icon", "logo", "avatar",
            ".thumbnails", ".trash", ".bin"
        )
    }

    /**
     * Smart directory discovery that filters out app-internal, asset, and other
     * non-user directories.  Only directories with ≥ 3 "meaningful" images
     * (≥ 100 KB file size AND ≥ 200 × 200 pixels) are returned.
     */
    override suspend fun discoverDirectories(): List<DirectoryInfo> = withContext(Dispatchers.IO) {
        val dirCounts = mutableMapOf<String, Int>()
        val projection = arrayOf(
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, null
        )?.use { cursor ->
            val relPathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)

                // --- Filter 1: minimum file size ---
                if (size < MIN_FILE_SIZE) continue

                // --- Filter 2: minimum image dimensions ---
                if (width < MIN_DIMENSION || height < MIN_DIMENSION) continue

                val relativePath = cursor.getString(relPathCol)
                val fullPath = cursor.getString(dataCol)

                val dir = when {
                    !relativePath.isNullOrBlank() -> relativePath.trimEnd('/')
                    !fullPath.isNullOrBlank() -> {
                        val idx = fullPath.indexOf("/Android/data/")
                        if (idx > 0) {
                            fullPath.substring(0, idx).substringAfterLast("emulated/0/")
                                .substringAfterLast("storage/emulated/0/")
                        } else {
                            fullPath.substringAfterLast("emulated/0/")
                                .substringAfterLast("storage/emulated/0/")
                        }.trimEnd('/')
                    }
                    else -> continue
                }

                if (dir.isBlank()) continue

                // --- Filter 3: exclude hidden directories (segments starting with '.') ---
                val segments = dir.split("/")
                if (segments.any { it.startsWith(".") }) continue

                // --- Filter 4: exclude known asset / non-user directories ---
                val dirLower = dir.lowercase()
                if (EXCLUDED_DIR_PATTERNS.any { pattern ->
                        segments.any { it.lowercase() == pattern } ||
                        dirLower.contains(pattern)
                    }) continue

                dirCounts[dir] = (dirCounts[dir] ?: 0) + 1
            }
        }

        // --- Filter 5: minimum image count per directory ---
        dirCounts.filter { it.value >= MIN_IMAGE_COUNT }
            .map { (path, count) ->
                DirectoryInfo(
                    relativePath = path,
                    displayName = path.substringAfterLast('/').ifBlank { path },
                    imageCount = count
                )
            }.sortedByDescending { it.imageCount }
    }

    override suspend fun classifyPhoto(photo: Photo): Photo {
        val apiKey = settingsRepository.getApiKeySync()
        if (apiKey.isBlank()) return photo
        val model = settingsRepository.getModelSync()
        val (classification, confidence, category) = openAIService.classifyPhoto(photo.uri, apiKey, model)
        return photo.copy(
            classification = classification,
            confidence = confidence,
            category = category
        )
    }

    override suspend fun updateClassification(
        photoId: Long, classification: Classification, confidence: Float, category: String
    ) {
        photoDao.updateClassification(photoId, classification.name, confidence, category)
    }

    override suspend fun deletePhotos(photos: List<Photo>) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val ids = photos.map { it.id }
            if (ids.isNotEmpty()) {
                photoDao.setTrashStatus(ids, true)
            }
            return
        }

        val trashDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "trash")
        if (!trashDir.exists()) trashDir.mkdirs()

        val ids = mutableListOf<Long>()
        photos.forEach { photo ->
            try {
                val uri = Uri.parse(photo.uri)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val trashFile = File(trashDir, "${photo.id}_${photo.displayName}")
                    trashFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()

                    context.contentResolver.delete(uri, null, null)
                    ids.add(photo.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoRepositoryImpl", "Failed to delete photo: ${photo.displayName}", e)
            }
        }

        if (ids.isNotEmpty()) {
            photoDao.setTrashStatus(ids, true)
        }
    }

    override suspend fun restorePhotos(photos: List<Photo>) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val ids = photos.map { it.id }
            if (ids.isNotEmpty()) {
                photoDao.setTrashStatus(ids, false)
            }
            return
        }

        val trashDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "trash")
        val ids = mutableListOf<Long>()

        photos.forEach { photo ->
            try {
                val trashFile = File(trashDir, "${photo.id}_${photo.displayName}")
                if (trashFile.exists()) {
                    if (photo.filePath.isNotBlank()) {
                        val originalFile = File(photo.filePath)
                        originalFile.parentFile?.mkdirs()
                        trashFile.copyTo(originalFile, overwrite = true)
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(photo.filePath), null, null)
                    }
                    ids.add(photo.id)
                    trashFile.delete()
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoRepositoryImpl", "Failed to restore photo: ${photo.displayName}", e)
            }
        }

        if (ids.isNotEmpty()) {
            photoDao.setTrashStatus(ids, false)
        }
    }

    override suspend fun createTrashPendingIntent(photos: List<Photo>): android.app.PendingIntent? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val uris = photos.map { Uri.parse(it.uri) }
            return try {
                MediaStore.createTrashRequest(context.contentResolver, uris, true)
            } catch (e: Exception) {
                android.util.Log.e("PhotoRepositoryImpl", "Failed to create trash request", e)
                null
            }
        }
        return null
    }

    override suspend fun getPhotoById(id: Long): Photo? =
        photoDao.getPhotoById(id)?.toDomain()

    override suspend fun getAllPhotoIds(): List<Long> =
        photoDao.getAllPhotoIds()

    override suspend fun deletePhotosByIds(ids: List<Long>) {
        photoDao.deleteByIds(ids)
    }

    override suspend fun insertPhotos(photos: List<Photo>) {
        photoDao.insertPhotos(photos.map { it.toEntity() })
    }

    override suspend fun clearAll() {
        photoDao.clearAll()
    }

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun detectLocalIssues(photo: Photo): Photo {
        return try {
            val uri = Uri.parse(photo.uri)
            
            // Step 1: Fast screenshot detection (no Bitmap decode needed)
            val isScreenshot = ScreenshotDetector.isScreenshot(
                context, uri, photo.displayName, photo.width, photo.height, photo.mimeType
            )
            
            if (isScreenshot) {
                return photo.copy(
                    isLocalUseless = true,
                    localReason = "截图",
                    classification = Classification.UNCERTAIN,
                    confidence = 0.7f,
                    category = "screenshot"
                )
            }

            // Step 2: Only decode Bitmap for actual photos that need blur/blank/MLKit detection
            val sampleSize = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                calculateInSampleSize(options, 800, 800)
            } ?: 1

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream2 ->
                BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            }

            if (bitmap == null) return photo

            try {
                // Check if blank (uniform color)
                val isBlank = ImageUtils.isBlank(bitmap)
                if (isBlank) {
                    return photo.copy(
                        isLocalUseless = true,
                        localReason = "空白照片",
                        classification = Classification.USELESS,
                        confidence = 0.95f,
                        category = "blank_photo"
                    )
                }

                // Check if blurry
                val isBlurry = BlurDetector.isBlurry(bitmap)
                if (isBlurry) {
                    return photo.copy(
                        isLocalUseless = true,
                        localReason = "模糊照片",
                        classification = Classification.USELESS,
                        confidence = 0.9f,
                        category = "blurry_photo"
                    )
                }

                // ML Kit Image Labeling (reuse labeler instance)
                var mlKitCategory = ""
                var mlKitClassification: Classification? = null
                try {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val labels = labeler.process(inputImage).await()
                    
                    val documentLabels = setOf("Receipt", "Document", "Text", "Font", "Barcode")
                    for (label in labels) {
                        if (label.confidence > 0.7f && documentLabels.contains(label.text)) {
                            mlKitCategory = label.text.lowercase()
                            mlKitClassification = Classification.UNCERTAIN
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val dHash = ImageUtils.computeDHash(bitmap)

                val resultPhoto = if (mlKitClassification != null) {
                    photo.copy(
                        isLocalUseless = true,
                        localReason = "文档票据",
                        classification = Classification.UNCERTAIN,
                        confidence = 0.8f,
                        category = mlKitCategory
                    )
                } else {
                    photo
                }
                resultPhoto.copy(dHash = dHash)
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            photo
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun PhotoEntity.toDomain() = Photo(
        id = id, uri = uri, displayName = displayName, mimeType = mimeType,
        width = width, height = height, size = size, dateAdded = dateAdded,
        dateModified = dateModified, filePath = filePath,
        classification = try { Classification.valueOf(classification) } catch (_: Exception) { Classification.UNKNOWN },
        confidence = confidence, category = category,
        isLocalUseless = isLocalUseless, localReason = localReason, isInTrash = isInTrash,
        dHash = dHash
    )

    private fun Photo.toEntity() = PhotoEntity(
        id = id, uri = uri, displayName = displayName, mimeType = mimeType,
        width = width, height = height, size = size, dateAdded = dateAdded,
        dateModified = dateModified, filePath = filePath, classification = classification.name,
        confidence = confidence, category = category,
        isLocalUseless = isLocalUseless, localReason = localReason, isInTrash = isInTrash,
        dHash = dHash
    )
}

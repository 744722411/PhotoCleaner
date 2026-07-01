package com.photocleaner.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import com.photocleaner.data.local.PhotoDao
import com.photocleaner.data.mapper.PhotoMapper
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.DirectoryInfo
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.util.MediaAccessLevel
import com.photocleaner.util.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    private val mapper: PhotoMapper,
    @ApplicationContext private val context: Context
) : PhotoRepository {

    override fun getAllPhotos(): Flow<List<Photo>> =
        photoDao.getAllPhotos().map { entities -> entities.map { mapper.toDomain(it) } }

    override fun getPhotosByClassification(classification: Classification): Flow<List<Photo>> =
        photoDao.getPhotosByClassification(classification.name).map { entities ->
            entities.map { mapper.toDomain(it) }
        }

    override fun getClassifiedCount(): Flow<Int> = photoDao.getClassifiedCount()
    override fun getTotalCount(): Flow<Int> = photoDao.getTotalCount()
    override fun getUselessCount(): Flow<Int> = photoDao.getUselessCount()
    override fun getUselessSize(): Flow<Long> = photoDao.getUselessSize()
    override suspend fun getUselessCountSync(): Int = photoDao.getUselessCountSync()
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
            MediaStore.Images.Media.DATE_MODIFIED
        ) + mediaPathProjection()

        val useDirectoryFilter = selectedDirectories.isNotEmpty()
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        queryImages(projection, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                -1
            }
            val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            } else {
                -1
            }

            if (idCol < 0 || nameCol < 0 || mimeCol < 0 || widthCol < 0 || heightCol < 0 || sizeCol < 0 || dateAddedCol < 0 || dateModCol < 0) {
                return@use
            }

            val normalizedSelectedDirectories = selectedDirectories.mapTo(mutableSetOf()) { it.normalizedDirectory() }

            while (cursor.moveToNext()) {
                if (useDirectoryFilter) {
                    val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty().normalizedDirectory() else ""
                    val fullPath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty().normalizedDirectory() else ""
                    val matchesAny = normalizedSelectedDirectories.any { dir ->
                        relativePath == dir ||
                        relativePath.startsWith("$dir/", ignoreCase = true) ||
                        fullPath.contains("/$dir/", ignoreCase = true) ||
                        fullPath.endsWith("/$dir", ignoreCase = true)
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
                        filePath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    )
                )
            }
        }
        photos
    }

    companion object {
        private const val MIN_IMAGE_COUNT = 1
        private const val MIN_FILE_SIZE = 10 * 1024L
        private const val MIN_DIMENSION = 100
        private val EXCLUDED_DIR_PATTERNS = listOf(
            "drawable", "assets", "res", "mipmap",
            "emoji", "sticker", "emoticon",
            "wallpaper", "background", "launcher",
            "cache", "temp", "tmp", "thumb",
            "icon", "logo", "avatar",
            ".thumbnails", ".trash", ".bin"
        )
    }

    override suspend fun discoverDirectories(): List<DirectoryInfo> = withContext(Dispatchers.IO) {
        val dirCounts = mutableMapOf<String, Int>()
        val projection = arrayOf(
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        ) + mediaPathProjection()

        queryImages(projection, null)?.use { cursor ->
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                -1
            }
            val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            } else {
                -1
            }
            val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

            if (sizeCol < 0 || widthCol < 0 || heightCol < 0) return@use

            while (cursor.moveToNext()) {
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                if (size < MIN_FILE_SIZE) continue
                if (width < MIN_DIMENSION || height < MIN_DIMENSION) continue

                val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                val fullPath = if (dataCol >= 0) cursor.getString(dataCol) else null

                val dir = when {
                    !relativePath.isNullOrBlank() -> relativePath.trim('/')
                    !fullPath.isNullOrBlank() -> {
                        val parentPath = File(fullPath).parent ?: ""
                        if (parentPath.isNotBlank()) {
                            parentPath.substringAfter("emulated/0/")
                                .substringAfter("storage/emulated/0/")
                                .trim('/')
                        } else ""
                    }
                    else -> continue
                }
                if (dir.isBlank()) continue

                val segments = dir.split("/")
                if (segments.any { it.startsWith(".") }) continue

                val dirLower = dir.lowercase()
                if (EXCLUDED_DIR_PATTERNS.any { pattern ->
                        segments.any { it.lowercase() == pattern } || dirLower.contains(pattern)
                    }) continue

                dirCounts[dir] = (dirCounts[dir] ?: 0) + 1
            }
        }

        dirCounts.filter { it.value >= MIN_IMAGE_COUNT }
            .map { (path, count) ->
                DirectoryInfo(
                    relativePath = path,
                    displayName = path.substringAfterLast('/').ifBlank { path },
                    imageCount = count
                )
            }.sortedByDescending { it.imageCount }
    }

    override suspend fun updateClassification(
        photoId: Long, classification: Classification, confidence: Float, category: String
    ) {
        photoDao.updateClassification(photoId, classification.name, confidence, category)
    }

    override suspend fun deletePhotos(photos: List<Photo>) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val ids = photos.map { it.id }
            if (ids.isNotEmpty()) photoDao.setTrashStatus(ids, true)
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
                    trashFile.outputStream().use { output -> inputStream.copyTo(output) }
                    inputStream.close()
                    context.contentResolver.delete(uri, null, null)
                    ids.add(photo.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("PhotoRepositoryImpl", "Failed to delete photo: ${photo.displayName}", e)
            }
        }
        if (ids.isNotEmpty()) photoDao.setTrashStatus(ids, true)
    }

    override suspend fun restorePhotos(photos: List<Photo>) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val ids = photos.map { it.id }
            if (ids.isNotEmpty()) photoDao.setTrashStatus(ids, false)
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
        if (ids.isNotEmpty()) photoDao.setTrashStatus(ids, false)
    }

    override suspend fun getPhotoById(id: Long): Photo? = photoDao.getPhotoById(id)?.let { mapper.toDomain(it) }

    override suspend fun getAllPhotoIds(): List<Long> = photoDao.getAllPhotoIds()

    override suspend fun deletePhotosByIds(ids: List<Long>) = photoDao.deleteByIds(ids)

    override suspend fun insertPhotos(photos: List<Photo>) {
        photoDao.insertPhotos(photos.map { mapper.toEntity(it) })
    }

    override suspend fun clearAll() = photoDao.clearAll()

    private fun mediaPathProjection(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
        } else {
            arrayOf(MediaStore.Images.Media.DATA)
        }

    private fun String.normalizedDirectory(): String =
        replace('\\', '/')
            .trim()
            .trim('/')

    private fun queryImages(
        projection: Array<String>,
        sortOrder: String?
    ): Cursor? = if (
        PermissionHelper.getMediaAccessLevel(context) == MediaAccessLevel.PARTIAL &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    ) {
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            Bundle().apply {
                putBoolean(MediaStore.QUERY_ARG_LATEST_SELECTION_ONLY, true)
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
                )
                if (sortOrder != null) {
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                }
            },
            null
        )
    } else {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
    }
}

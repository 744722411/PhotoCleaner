package com.photocleaner.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    private val mapper: PhotoMapper,
    @param:ApplicationContext private val context: Context
) : PhotoRepository {

    override fun getAllPhotos(): Flow<List<Photo>> =
        photoDao.getAllPhotos().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getAllPhotosSync(): List<Photo> = withContext(Dispatchers.IO) {
        photoDao.getAllPhotosSync().map { mapper.toDomain(it) }
    }

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

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        queryImages(projection, sortOrder, selectedDirectories).forEach { cursor -> cursor.use {
            val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
            val relPathCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)

            if (idCol < 0 || nameCol < 0 || mimeCol < 0 || widthCol < 0 || heightCol < 0 || sizeCol < 0 || dateAddedCol < 0 || dateModCol < 0) {
                return@use
            }

            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idCol)
                val volumeName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME))
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.getContentUri(volumeName), mediaStoreId
                ).toString()
                // Preserve existing primary-volume IDs while giving removable volumes a separate key space.
                val id = if (volumeName == MediaStore.VOLUME_EXTERNAL_PRIMARY) mediaStoreId else stableVolumeId(uri)

                val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty().normalizedDirectory() else ""

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
                        filePath = relativePath
                    )
                )
            }
        } }
        photos
    }

    companion object {
        private const val MIN_IMAGE_COUNT = 1
        private const val MIN_FILE_SIZE = 10 * 1024L
        private const val MIN_DIMENSION = 100
        private const val DELETE_VERIFY_ATTEMPTS = 6
        private const val DELETE_VERIFY_DELAY_MS = 250L
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

        queryImages(projection, null, emptySet()).forEach { cursor -> cursor.use {
            val relPathCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
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
                val dir = when {
                    !relativePath.isNullOrBlank() -> relativePath.trim('/')
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
        } }

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
        val ids = photos.map { it.id }
        if (ids.isNotEmpty()) {
            ids.chunked(900).forEach { chunk ->
                photoDao.setTrashStatus(chunk, true)
            }
        }
    }

    override suspend fun findDeletedPhotoIds(photos: List<Photo>): List<Long> = withContext(Dispatchers.IO) {
        val remaining = photos.toMutableList()
        val deletedIds = mutableListOf<Long>()
        repeat(DELETE_VERIFY_ATTEMPTS) { attempt ->
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val photo = iterator.next()
                val isDeleted = runCatching {
                    context.contentResolver.query(
                        Uri.parse(photo.uri),
                        arrayOf(MediaStore.MediaColumns._ID),
                        null,
                        null,
                        null
                    )?.use { !it.moveToFirst() } ?: true
                }.getOrDefault(false)
                if (isDeleted) {
                    deletedIds += photo.id
                    iterator.remove()
                }
            }
            if (remaining.isEmpty()) return@withContext deletedIds
            if (attempt < DELETE_VERIFY_ATTEMPTS - 1) delay(DELETE_VERIFY_DELAY_MS)
        }
        deletedIds
    }

    override fun hasFullMediaAccess(): Boolean =
        PermissionHelper.getMediaAccessLevel(context) == MediaAccessLevel.FULL

    override suspend fun getPhotoById(id: Long): Photo? = photoDao.getPhotoById(id)?.let { mapper.toDomain(it) }

    override suspend fun getAllPhotoIds(): List<Long> = photoDao.getAllPhotoIds()

    override suspend fun getActivePhotoIds(): List<Long> = photoDao.getActivePhotoIds()

    override suspend fun clearTrashStatus(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            ids.chunked(900).forEach { chunk ->
                photoDao.clearTrashStatus(chunk)
            }
        }
    }

    override suspend fun deletePhotosByIds(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            ids.chunked(900).forEach { chunk ->
                photoDao.deleteByIds(chunk)
            }
        }
    }

    override suspend fun insertPhotos(photos: List<Photo>) {
        photoDao.insertPhotos(photos.map { mapper.toEntity(it) })
    }

    override suspend fun clearAll() = photoDao.clearAll()

    private fun mediaPathProjection(): Array<String> =
        arrayOf(MediaStore.Images.Media.RELATIVE_PATH)

    private fun String.normalizedDirectory(): String =
        replace('\\', '/')
            .trim()
            .trim('/')

    private fun stableVolumeId(uri: String): Long {
        var hash = -0x340d631b7bdddcdbL
        uri.forEach { character ->
            hash = (hash xor character.code.toLong()) * 0x100000001b3L
        }
        return hash or Long.MIN_VALUE
    }

    private fun queryImages(
        projection: Array<String>,
        sortOrder: String?,
        selectedDirectories: Set<String>
    ): List<Cursor> {
        val directories = selectedDirectories.map { it.normalizedDirectory() }.filter { it.isNotEmpty() }
        val selection = directories.takeIf { it.isNotEmpty() }?.joinToString(" OR ") {
            "(${MediaStore.Images.Media.RELATIVE_PATH} = ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? ESCAPE '\\')"
        }
        val selectionArgs = directories.flatMap { directory ->
            val escaped = directory
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            listOf("$directory/", "$escaped/%")
        }.toTypedArray()
        val queryArgs = Bundle().apply {
            if (selection != null) {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            }
            if (sortOrder != null) putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
        }
        return MediaStore.getExternalVolumeNames(context).mapNotNull { volume ->
            context.contentResolver.query(
                MediaStore.Images.Media.getContentUri(volume),
                projection + MediaStore.MediaColumns.VOLUME_NAME,
                queryArgs,
                null
            )
        }
    }
}

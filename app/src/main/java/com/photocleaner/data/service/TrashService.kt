package com.photocleaner.data.service

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.photocleaner.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Platform-coupled service that builds Android [PendingIntent]s for the system
 * trash flow. Deliberately kept out of the pure domain [PhotoRepository] so the
 * domain layer does not depend on Android framework types.
 */
interface TrashService {
    suspend fun createTrashPendingIntent(photos: List<Photo>): PendingIntent?
}

@Singleton
class MediaStoreTrashService @Inject constructor(
    @ApplicationContext private val context: Context
) : TrashService {

    override suspend fun createTrashPendingIntent(photos: List<Photo>): PendingIntent? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return null
        val uris = photos.map { Uri.parse(it.uri) }
        return try {
            MediaStore.createTrashRequest(context.contentResolver, uris, true)
        } catch (_: Exception) {
            null
        }
    }
}

package com.photocleaner.data.mapper

import com.photocleaner.data.local.entity.PhotoEntity
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoMapper @Inject constructor() {
    fun toDomain(entity: PhotoEntity): Photo = Photo(
        id = entity.id,
        uri = entity.uri,
        displayName = entity.displayName,
        mimeType = entity.mimeType,
        width = entity.width,
        height = entity.height,
        size = entity.size,
        dateAdded = entity.dateAdded,
        dateModified = entity.dateModified,
        filePath = entity.filePath,
        classification = Classification.fromString(entity.classification),
        confidence = entity.confidence,
        category = entity.category,
        isLocalUseless = entity.isLocalUseless,
        localReason = entity.localReason,
        isInTrash = entity.isInTrash,
        dHash = entity.dHash
    )

    fun toEntity(photo: Photo): PhotoEntity = PhotoEntity(
        id = photo.id,
        uri = photo.uri,
        displayName = photo.displayName,
        mimeType = photo.mimeType,
        width = photo.width,
        height = photo.height,
        size = photo.size,
        dateAdded = photo.dateAdded,
        dateModified = photo.dateModified,
        filePath = photo.filePath,
        classification = photo.classification.name,
        confidence = photo.confidence,
        category = photo.category,
        isLocalUseless = photo.isLocalUseless,
        localReason = photo.localReason,
        isInTrash = photo.isInTrash,
        dHash = photo.dHash
    )
}

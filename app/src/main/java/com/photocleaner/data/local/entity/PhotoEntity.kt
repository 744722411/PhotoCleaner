package com.photocleaner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [
        Index("classification"),
        Index("isInTrash")
    ]
)
data class PhotoEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val classification: String = "UNKNOWN",
    val confidence: Float = 0f,
    val category: String = "",
    val isLocalUseless: Boolean = false,
    val localReason: String = "",
    val isInTrash: Boolean = false
)

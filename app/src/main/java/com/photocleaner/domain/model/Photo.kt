package com.photocleaner.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Photo(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val filePath: String = "",
    val classification: Classification = Classification.UNKNOWN,
    val confidence: Float = 0f,
    val category: String = "",
    val isLocalUseless: Boolean = false,
    val localReason: String = "",
    val isInTrash: Boolean = false,
    val dHash: Long = 0L
)


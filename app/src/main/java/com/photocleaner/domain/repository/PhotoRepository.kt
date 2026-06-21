package com.photocleaner.domain.repository

import com.photocleaner.domain.model.DirectoryInfo
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getAllPhotos(): Flow<List<Photo>>
    fun getPhotosByClassification(classification: Classification): Flow<List<Photo>>
    fun getClassifiedCount(): Flow<Int>
    fun getTotalCount(): Flow<Int>
    fun getUselessCount(): Flow<Int>
    fun getUselessSize(): Flow<Long>
    suspend fun scanPhotos(): List<Photo>
    suspend fun scanPhotos(selectedDirectories: Set<String>): List<Photo>
    suspend fun discoverDirectories(): List<DirectoryInfo>
    suspend fun classifyPhoto(photo: Photo): Photo
    suspend fun updateClassification(photoId: Long, classification: Classification, confidence: Float, category: String)
    suspend fun deletePhotos(photos: List<Photo>)
    suspend fun restorePhotos(photos: List<Photo>)
    suspend fun getPhotoById(id: Long): Photo?
    suspend fun getAllPhotoIds(): List<Long>
    suspend fun deletePhotosByIds(ids: List<Long>)
    suspend fun insertPhotos(photos: List<Photo>)
    suspend fun clearAll()
    suspend fun detectLocalIssues(photo: Photo): Photo
}

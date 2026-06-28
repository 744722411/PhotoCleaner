package com.photocleaner.data.local

import androidx.room.*
import com.photocleaner.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE isInTrash = 0 ORDER BY dateAdded DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE classification = :classification AND isInTrash = 0 ORDER BY dateAdded DESC")
    fun getPhotosByClassification(classification: String): Flow<List<PhotoEntity>>

    @Query("SELECT COUNT(*) FROM photos WHERE classification != 'UNKNOWN' AND isInTrash = 0")
    fun getClassifiedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE isInTrash = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE classification = 'USELESS' AND isInTrash = 0")
    fun getUselessCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE classification = 'USELESS' AND isInTrash = 0")
    suspend fun getUselessCountSync(): Int

    @Query("SELECT COALESCE(SUM(size), 0) FROM photos WHERE classification = 'USELESS' AND isInTrash = 0")
    fun getUselessSize(): Flow<Long>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("UPDATE photos SET classification = :classification, confidence = :confidence, category = :category WHERE id = :id")
    suspend fun updateClassification(id: Long, classification: String, confidence: Float, category: String)

    @Query("UPDATE photos SET isInTrash = :inTrash WHERE id IN (:ids)")
    suspend fun setTrashStatus(ids: List<Long>, inTrash: Boolean)

    @Query("DELETE FROM photos")
    suspend fun clearAll()

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT id FROM photos")
    suspend fun getAllPhotoIds(): List<Long>

    @Query("SELECT * FROM photos WHERE isInTrash = 1")
    suspend fun getTrashedPhotos(): List<PhotoEntity>
}

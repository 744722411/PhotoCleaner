package com.photocleaner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photocleaner.data.local.entity.PhotoEntity

@Database(entities = [PhotoEntity::class], version = 3, exportSchema = true)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
}

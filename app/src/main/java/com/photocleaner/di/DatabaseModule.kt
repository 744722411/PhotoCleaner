package com.photocleaner.di

import android.content.Context
import androidx.room.Room
import com.photocleaner.data.local.PhotoDao
import com.photocleaner.data.local.PhotoDatabase
import com.photocleaner.data.local.PhotoDatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhotoDatabase =
        Room.databaseBuilder(context, PhotoDatabase::class.java, "photo_cleaner.db")
            .addMigrations(*PhotoDatabaseMigrations.ALL)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun providePhotoDao(database: PhotoDatabase): PhotoDao = database.photoDao()
}

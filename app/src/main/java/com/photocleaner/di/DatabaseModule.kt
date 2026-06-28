package com.photocleaner.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.photocleaner.data.local.PhotoDao
import com.photocleaner.data.local.PhotoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE photos ADD COLUMN isLocalUseless INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE photos ADD COLUMN localReason TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE photos ADD COLUMN isInTrash INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE photos ADD COLUMN dHash INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_isInTrash` ON `photos` (`isInTrash`)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhotoDatabase =
        Room.databaseBuilder(context, PhotoDatabase::class.java, "photo_cleaner.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // Allow destructive fallback for both upgrades and downgrades
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePhotoDao(database: PhotoDatabase): PhotoDao = database.photoDao()
}

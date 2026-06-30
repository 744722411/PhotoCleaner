package com.photocleaner.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrateFrom1To4() {
        createVersion1Database()
        openMigratedDatabase()
    }

    @Test
    fun migrateFrom2To4() {
        createVersion2Database()
        openMigratedDatabase()
    }

    @Test
    fun migrateFrom3To4() {
        createVersion3Database()
        openMigratedDatabase()
    }

    private fun openMigratedDatabase() {
        Room.databaseBuilder(context, PhotoDatabase::class.java, TEST_DB)
            .addMigrations(*PhotoDatabaseMigrations.ALL)
            .build()
            .use { database ->
                database.openHelper.writableDatabase.query("SELECT * FROM photos LIMIT 0").close()
            }
    }

    private fun createVersion1Database() {
        createDatabase(version = 1) { db ->
            db.execSQL(
                """
                CREATE TABLE photos (
                    id INTEGER NOT NULL,
                    uri TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    size INTEGER NOT NULL,
                    dateAdded INTEGER NOT NULL,
                    dateModified INTEGER NOT NULL,
                    filePath TEXT NOT NULL,
                    classification TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    category TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
        }
    }

    private fun createVersion2Database() {
        createDatabase(version = 2) { db ->
            db.execSQL(
                """
                CREATE TABLE photos (
                    id INTEGER NOT NULL,
                    uri TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    size INTEGER NOT NULL,
                    dateAdded INTEGER NOT NULL,
                    dateModified INTEGER NOT NULL,
                    filePath TEXT NOT NULL,
                    classification TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    category TEXT NOT NULL,
                    isLocalUseless INTEGER NOT NULL DEFAULT 0,
                    localReason TEXT NOT NULL DEFAULT '',
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
        }
    }

    private fun createVersion3Database() {
        createDatabase(version = 3) { db ->
            db.execSQL(
                """
                CREATE TABLE photos (
                    id INTEGER NOT NULL,
                    uri TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    size INTEGER NOT NULL,
                    dateAdded INTEGER NOT NULL,
                    dateModified INTEGER NOT NULL,
                    filePath TEXT NOT NULL,
                    classification TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    category TEXT NOT NULL,
                    isLocalUseless INTEGER NOT NULL,
                    localReason TEXT NOT NULL,
                    isInTrash INTEGER NOT NULL,
                    dHash INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_classification ON photos (classification)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_isInTrash ON photos (isInTrash)")
        }
    }

    private fun createDatabase(version: Int, createSchema: (SQLiteDatabase) -> Unit) {
        context.deleteDatabase(TEST_DB)
        val file = context.getDatabasePath(TEST_DB)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createSchema(db)
            db.version = version
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}

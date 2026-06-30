package com.photocleaner.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PhotoDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE photos ADD COLUMN isLocalUseless INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE photos ADD COLUMN localReason TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE photos ADD COLUMN isInTrash INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE photos ADD COLUMN dHash INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_classification` ON `photos` (`classification`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_isInTrash` ON `photos` (`isInTrash`)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_classification` ON `photos` (`classification`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_isInTrash` ON `photos` (`isInTrash`)")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}

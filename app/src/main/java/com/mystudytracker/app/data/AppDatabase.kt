package com.mystudytracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DailyProgress::class, DailyAttachment::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyProgressDao(): DailyProgressDao
    abstract fun dailyAttachmentDao(): DailyAttachmentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** Adds the "locked" column backing the permanent day-finalization feature. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_progress ADD COLUMN locked INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds the "note" column backing the per-day note feature. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_progress ADD COLUMN note TEXT DEFAULT NULL")
            }
        }

        /** Creates the daily_attachments table backing the Remark & Attachments feature. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_attachments (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date        TEXT    NOT NULL,
                        filePath    TEXT    NOT NULL,
                        type        TEXT    NOT NULL,
                        displayName TEXT    NOT NULL,
                        addedAt     INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds the Homework section's 3 columns (Physics/Chemistry/Biology), default unchecked. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_progress ADD COLUMN homeworkPhysics INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_progress ADD COLUMN homeworkChemistry INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_progress ADD COLUMN homeworkBiology INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mystudytracker.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

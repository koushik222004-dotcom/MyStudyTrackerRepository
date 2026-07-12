package com.mystudytracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DailyProgress::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyProgressDao(): DailyProgressDao

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

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mystudytracker.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}

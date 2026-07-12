package com.mystudytracker.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyProgressDao {

    @Query("SELECT * FROM daily_progress WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DailyProgress?>

    @Query("SELECT * FROM daily_progress")
    fun observeAll(): Flow<List<DailyProgress>>

    @Query("SELECT locked FROM daily_progress WHERE date = :date LIMIT 1")
    suspend fun isLocked(date: String): Boolean?

    @Query("UPDATE daily_progress SET locked = 1 WHERE date = :date")
    suspend fun setLocked(date: String)

    @Query("SELECT note FROM daily_progress WHERE date = :date LIMIT 1")
    suspend fun getNote(date: String): String?

    /** Returns the number of rows updated - 0 means no row exists yet for [date]. */
    @Query("UPDATE daily_progress SET note = :note WHERE date = :date")
    suspend fun updateNote(date: String, note: String?): Int

    @Upsert
    suspend fun upsert(progress: DailyProgress)
}

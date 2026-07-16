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

    @Query("UPDATE daily_progress SET locked = 1 WHERE date = :date")
    suspend fun setLocked(date: String)

    /** Returns the number of rows updated - 0 means no row exists yet for [date]. */
    @Query("UPDATE daily_progress SET note = :note WHERE date = :date")
    suspend fun updateNote(date: String, note: String?): Int

    @Upsert
    suspend fun upsert(progress: DailyProgress)

    /**
     * Overwrites just the denormalized completed/total unit counts for [date], preserving any
     * existing lock/note, or creates a fresh unlocked row with no note if none exists yet. Used
     * after every task-state change to keep the day's aggregate in sync (see
     * ProgressRepository.recomputeAggregate).
     */
    @Query(
        """
        INSERT INTO daily_progress (date, completedUnits, totalUnits, locked, note)
        VALUES (:date, :completedUnits, :totalUnits, 0, NULL)
        ON CONFLICT(date) DO UPDATE SET completedUnits = :completedUnits, totalUnits = :totalUnits
        """
    )
    suspend fun upsertAggregate(date: String, completedUnits: Int, totalUnits: Int)
}

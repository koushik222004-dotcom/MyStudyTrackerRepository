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

    @Upsert
    suspend fun upsert(progress: DailyProgress)
}

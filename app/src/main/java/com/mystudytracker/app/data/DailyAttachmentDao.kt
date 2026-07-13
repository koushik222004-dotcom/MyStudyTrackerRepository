package com.mystudytracker.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyAttachmentDao {

    @Query("SELECT * FROM daily_attachments WHERE date = :date ORDER BY addedAt ASC")
    fun observeByDate(date: String): Flow<List<DailyAttachment>>

    @Insert
    suspend fun insert(attachment: DailyAttachment): Long

    @Query("DELETE FROM daily_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Returns the stored file path so the caller can delete the file before removing the row. */
    @Query("SELECT filePath FROM daily_attachments WHERE id = :id LIMIT 1")
    suspend fun getFilePath(id: Long): String?
}

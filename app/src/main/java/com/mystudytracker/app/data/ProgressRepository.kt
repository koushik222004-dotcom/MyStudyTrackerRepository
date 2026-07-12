package com.mystudytracker.app.data

import kotlinx.coroutines.flow.Flow

/** Thin data-access layer between the ViewModels and Room - keeps DAO/SQL details out of the UI layer. */
class ProgressRepository(private val dao: DailyProgressDao) {

    fun observeAll(): Flow<List<DailyProgress>> = dao.observeAll()

    fun observeByDate(date: String): Flow<DailyProgress?> = dao.observeByDate(date)

    suspend fun saveDay(date: String, checked: Map<String, Boolean>) {
        // Preserve any existing lock state and note - this path is only ever reached for task
        // edits, and neither the lock status nor a saved note should be wiped out by a task save.
        val existingLocked = dao.isLocked(date) ?: false
        val existingNote = dao.getNote(date)
        dao.upsert(DailyProgress.fromTaskMap(date, checked, locked = existingLocked, note = existingNote))
    }

    /** Permanently finalizes a day. There is no corresponding "unlock" - this cannot be undone. */
    suspend fun lockDay(date: String) {
        dao.setLocked(date)
    }

    /**
     * Saves the free-form note for [date], independent of task state and lock status. Creates a
     * fresh row (all tasks unchecked) if [date] has no saved progress yet.
     */
    suspend fun saveNote(date: String, note: String?) {
        val rowsUpdated = dao.updateNote(date, note)
        if (rowsUpdated == 0) {
            dao.upsert(DailyProgress(date = date, note = note))
        }
    }
}

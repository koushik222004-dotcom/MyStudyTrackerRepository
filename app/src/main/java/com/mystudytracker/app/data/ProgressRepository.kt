package com.mystudytracker.app.data

import kotlinx.coroutines.flow.Flow

/** Thin data-access layer between the ViewModels and Room - keeps DAO/SQL details out of the UI layer. */
class ProgressRepository(private val dao: DailyProgressDao) {

    fun observeAll(): Flow<List<DailyProgress>> = dao.observeAll()

    fun observeByDate(date: String): Flow<DailyProgress?> = dao.observeByDate(date)

    suspend fun saveDay(date: String, checked: Map<String, Boolean>, locked: Boolean, note: String?) {
        dao.upsert(DailyProgress.fromTaskMap(date, checked, locked = locked, note = note))
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

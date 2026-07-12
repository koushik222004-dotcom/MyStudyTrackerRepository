package com.mystudytracker.app.data

import kotlinx.coroutines.flow.Flow

/** Thin data-access layer between the ViewModels and Room - keeps DAO/SQL details out of the UI layer. */
class ProgressRepository(private val dao: DailyProgressDao) {

    fun observeAll(): Flow<List<DailyProgress>> = dao.observeAll()

    fun observeByDate(date: String): Flow<DailyProgress?> = dao.observeByDate(date)

    suspend fun saveDay(date: String, checked: Map<String, Boolean>) {
        // Preserve any existing lock state - this path is only ever reached for edits, and a
        // locked day must never have its tasks rewritten unlocked by a stale save.
        val existingLocked = dao.isLocked(date) ?: false
        dao.upsert(DailyProgress.fromTaskMap(date, checked, locked = existingLocked))
    }

    /** Permanently finalizes a day. There is no corresponding "unlock" - this cannot be undone. */
    suspend fun lockDay(date: String) {
        dao.setLocked(date)
    }
}

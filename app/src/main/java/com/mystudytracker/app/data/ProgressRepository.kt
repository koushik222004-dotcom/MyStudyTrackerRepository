package com.mystudytracker.app.data

import kotlinx.coroutines.flow.Flow

/** Thin data-access layer between the ViewModels and Room - keeps DAO/SQL details out of the UI layer. */
class ProgressRepository(private val dao: DailyProgressDao) {

    fun observeAll(): Flow<List<DailyProgress>> = dao.observeAll()

    fun observeByDate(date: String): Flow<DailyProgress?> = dao.observeByDate(date)

    suspend fun saveDay(date: String, checked: Map<String, Boolean>) {
        dao.upsert(DailyProgress.fromTaskMap(date, checked))
    }
}

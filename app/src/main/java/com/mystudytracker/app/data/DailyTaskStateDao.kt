package com.mystudytracker.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One aggregated row per task key, summed across every touched day up to a given date - see
 *  [DailyTaskStateDao.backlogThrough]. Days with no saved row for a task aren't included here;
 *  the repository adds their default (1 pending each) on top using [touchedDays]. */
data class TaskBacklogRow(
    val taskKey: String,
    val pendingFromRows: Int,
    val touchedDays: Int
)

@Dao
interface DailyTaskStateDao {

    @Query("SELECT * FROM daily_task_state WHERE date = :date")
    fun observeByDate(date: String): Flow<List<DailyTaskState>>

    @Query("SELECT * FROM daily_task_state WHERE date = :date")
    suspend fun getByDate(date: String): List<DailyTaskState>

    @Upsert
    suspend fun upsert(state: DailyTaskState)

    @Query("SELECT * FROM daily_task_state")
    suspend fun getAll(): List<DailyTaskState>

    @Query("DELETE FROM daily_task_state")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertAll(states: List<DailyTaskState>)

    /**
     * One row per task key that has ever been touched on or before [throughDate]: the total
     * pending units contributed by those saved rows, and how many days contributed a row at all
     * (so the caller can add "1 pending" for every day in range that has no row, i.e. untouched
     * days default to a single outstanding unit).
     */
    @Query(
        """
        SELECT taskKey,
               SUM(CASE WHEN notApplicable = 1 THEN 0 ELSE targetCount - completedCount END) AS pendingFromRows,
               COUNT(*) AS touchedDays
        FROM daily_task_state
        WHERE date <= :throughDate
        GROUP BY taskKey
        """
    )
    suspend fun backlogThrough(throughDate: String): List<TaskBacklogRow>


}

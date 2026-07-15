package com.mystudytracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per tracked day, keyed by ISO date string (e.g. "2026-07-08"). Per-task state now lives
 * in the normalized [DailyTaskState] table; this row only holds day-level metadata plus a
 * denormalized [completedUnits]/[totalUnits] pair so the calendar and progress bar can render
 * every day's fraction without re-reading and re-summing every leaf task for every visible cell.
 * Both fields are recomputed from [DailyTaskState] (see ProgressRepository.recomputeAggregate)
 * every time a task on that date changes, so they can never drift out of sync.
 */
@Entity(tableName = "daily_progress")
data class DailyProgress(
    @PrimaryKey val date: String,

    /** Sum of completed units across every non-excluded leaf task for this day. */
    val completedUnits: Int = 0,

    /** Sum of target units across every non-excluded leaf task for this day (the denominator). */
    val totalUnits: Int = TaskCatalog.totalLeafCount,

    /** Once true, the day is permanently finalized: tasks can never be toggled again. */
    val locked: Boolean = false,

    /** Free-form note for the day, edited independently of task state and lock status. */
    val note: String? = null
)

package com.mystudytracker.app.data

import androidx.room.Entity

/**
 * Per-day state for a single leaf task, keyed by ([date], [taskKey]) where [taskKey] is the full
 * dot-joined path from [TaskCatalog] (e.g. "lectures.chemistry.organic"). Only rows that deviate
 * from the default are ever written - a leaf with no row for a given date is implicitly
 * "pending, quantity 1, applies" (see [effectiveTarget]/[effectiveCompleted]).
 *
 * This normalized table (one row per touched task per day) replaces the old design of one fixed
 * Room column per task. That worked when the catalog was small and static, but every catalog
 * change (adding a section, splitting a subject) required a matching schema migration touching
 * several files at once. Storing state by task *key* instead means the catalog can grow, shrink,
 * or reshape freely - only a genuine key rename invalidates history for that specific task.
 */
@Entity(tableName = "daily_task_state", primaryKeys = ["date", "taskKey"])
data class DailyTaskState(
    val date: String,
    val taskKey: String,
    /** How many of [targetCount] units are done. Ignored (treated as 0) while [notApplicable]. */
    val completedCount: Int = 0,
    /** How many units this task represents that day (e.g. "2" for two lectures). Default 1. */
    val targetCount: Int = 1,
    /** True if this task did not apply that day (e.g. no test). Excluded from totals and backlog. */
    val notApplicable: Boolean = false
) {
    companion object {
        /** The implicit state of any leaf with no saved row: pending, quantity 1, applies. */
        fun default(date: String, taskKey: String) = DailyTaskState(date, taskKey)
    }
}

/** Units this row contributes to a day's denominator - 0 if not applicable, else [DailyTaskState.targetCount]. */
fun DailyTaskState?.effectiveTarget(): Int = if (this == null) 1 else if (notApplicable) 0 else targetCount

/** Units this row contributes to a day's completed count - 0 if not applicable, else [DailyTaskState.completedCount]. */
fun DailyTaskState?.effectiveCompleted(): Int = if (this == null) 0 else if (notApplicable) 0 else completedCount

/** True when every unit of this leaf is done (and it isn't excluded). A row-less leaf is never done. */
fun DailyTaskState?.isDone(): Boolean = this != null && !notApplicable && completedCount >= targetCount && targetCount > 0

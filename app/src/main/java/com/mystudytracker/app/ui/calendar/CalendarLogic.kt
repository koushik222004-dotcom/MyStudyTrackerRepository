package com.mystudytracker.app.ui.calendar

import com.mystudytracker.app.data.TaskCatalog
import com.mystudytracker.app.util.DateRules
import java.time.LocalDate

/** Visual state of one calendar cell - drives both its color and whether it can be tapped. */
enum class DayStatus {
    OUTSIDE, // before START_DATE or after END_DATE - not rendered
    FUTURE, // after today - locked, cannot be opened
    TODAY_INCOMPLETE,
    TODAY_COMPLETE,
    GREEN, // past day, fully completed (all applicable tasks checked)
    YELLOW, // past day, partially completed
    RED // past day, nothing completed
}

/**
 * [totalUnits] is the day's own denominator, not a fixed catalog constant - "doesn't apply" and
 * quantities both shrink/grow it per day, so a day with every applicable task done can turn green
 * even if some tasks were excluded. A day with no saved row yet has never been opened, so it
 * defaults to the full catalog size with nothing completed (same "everything outstanding" reading
 * the app has always used for untouched days).
 */
fun computeDayStatus(date: LocalDate, today: LocalDate, completedUnits: Int?, totalUnits: Int?): DayStatus {
    if (!DateRules.isWithinTrackedRange(date)) return DayStatus.OUTSIDE
    if (date.isAfter(today)) return DayStatus.FUTURE

    val completed = completedUnits ?: 0
    val total = totalUnits ?: TaskCatalog.totalLeafCount
    if (date.isEqual(today)) {
        return if (total > 0 && completed >= total) DayStatus.TODAY_COMPLETE else DayStatus.TODAY_INCOMPLETE
    }
    return when {
        total > 0 && completed >= total -> DayStatus.GREEN
        completed <= 0 -> DayStatus.RED
        else -> DayStatus.YELLOW
    }
}

fun isDayClickable(status: DayStatus): Boolean =
    status != DayStatus.FUTURE && status != DayStatus.OUTSIDE

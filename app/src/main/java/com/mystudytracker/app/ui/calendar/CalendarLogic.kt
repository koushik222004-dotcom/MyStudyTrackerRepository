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
    GREEN, // past day, fully completed (25/25)
    YELLOW, // past day, partially completed
    RED // past day, nothing completed
}

fun computeDayStatus(date: LocalDate, today: LocalDate, completedCount: Int?): DayStatus {
    if (!DateRules.isWithinTrackedRange(date)) return DayStatus.OUTSIDE
    if (date.isAfter(today)) return DayStatus.FUTURE

    val count = completedCount ?: 0
    if (date.isEqual(today)) {
        return if (count >= TaskCatalog.totalTaskCount) DayStatus.TODAY_COMPLETE else DayStatus.TODAY_INCOMPLETE
    }
    return when {
        count >= TaskCatalog.totalTaskCount -> DayStatus.GREEN
        count <= 0 -> DayStatus.RED
        else -> DayStatus.YELLOW
    }
}

fun isDayClickable(status: DayStatus): Boolean =
    status != DayStatus.FUTURE && status != DayStatus.OUTSIDE

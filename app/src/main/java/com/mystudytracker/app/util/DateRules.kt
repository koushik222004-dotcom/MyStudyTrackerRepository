package com.mystudytracker.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * All date-based business rules for MyStudyTracker live here in one place:
 * - the fixed tracking window (29 Jun 2026 - 31 May 2027)
 * - the "alternate Sunday" rule that changes what Pre-Lecture Revision / Practice
 *   are supposed to cover every other Sunday.
 */
object DateRules {

    val START_DATE: LocalDate = LocalDate.of(2026, 6, 29)
    val END_DATE: LocalDate = LocalDate.of(2027, 5, 31)

    fun isWithinTrackedRange(date: LocalDate): Boolean =
        !date.isBefore(START_DATE) && !date.isAfter(END_DATE)

    /**
     * Number of Sundays from START_DATE up to and including [date] (0 if [date] is before
     * START_DATE). Used to alternate the revision/practice scope every other Sunday.
     *
     * Computed in O(1) by finding the first Sunday on or after START_DATE, then counting
     * complete 7-day intervals between it and [date].
     */
    fun sundayOrdinal(date: LocalDate): Int {
        if (date.isBefore(START_DATE)) return 0
        // DayOfWeek values: MONDAY=1 .. SUNDAY=7. Days until the next Sunday (0 if already Sunday).
        val daysToFirstSunday = (DayOfWeek.SUNDAY.value - START_DATE.dayOfWeek.value + 7) % 7
        val firstSunday = START_DATE.plusDays(daysToFirstSunday.toLong())
        if (date.isBefore(firstSunday)) return 0
        return (ChronoUnit.DAYS.between(firstSunday, date) / 7 + 1).toInt()
    }

    /** Scope text shown under "Pre-Lecture Revision" for the given date. */
    fun preLectureRule(date: LocalDate): String {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return "Previous 2 Lectures"
        return if (sundayOrdinal(date) % 2 == 0) "Complete Revision Till Date" else "Current + Previous Chapter"
    }

    /** Scope text shown under "Practice" for the given date. */
    fun practiceRule(date: LocalDate): String {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return "Current Chapter"
        return if (sundayOrdinal(date) % 2 == 0) "Complete Practice Till Date" else "Current + Previous Chapter"
    }
}

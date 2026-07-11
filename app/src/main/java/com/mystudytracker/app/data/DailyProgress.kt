package com.mystudytracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per tracked day, keyed by ISO date string (e.g. "2026-07-08").
 * There is exactly one boolean column per fixed task (25 total, see [TaskCatalog]),
 * plus a denormalized [completedCount] so the calendar screen can color days
 * without re-reading and re-counting 25 booleans for every visible cell.
 */
@Entity(tableName = "daily_progress")
data class DailyProgress(
    @PrimaryKey val date: String,

    val preLecturePhysics: Boolean = false,
    val preLectureChemistry: Boolean = false,
    val preLectureBiology: Boolean = false,

    val lecturesPhysics: Boolean = false,
    val lecturesChemistry: Boolean = false,
    val lecturesBiology: Boolean = false,
    val lecturesExtra: Boolean = false,

    val notesPhysics: Boolean = false,
    val notesChemistry: Boolean = false,
    val notesBiology: Boolean = false,

    val dppPhysics: Boolean = false,
    val dppChemistry: Boolean = false,
    val dppBiology: Boolean = false,

    val assignmentsPhysics: Boolean = false,
    val assignmentsChemistry: Boolean = false,
    val assignmentsBiology: Boolean = false,

    val postLecturePhysics: Boolean = false,
    val postLectureChemistry: Boolean = false,
    val postLectureBiology: Boolean = false,

    val practicePhysics: Boolean = false,
    val practiceChemistry: Boolean = false,
    val practiceBiology: Boolean = false,

    val ncertReading: Boolean = false,

    val testsTest: Boolean = false,
    val testsAnalysis: Boolean = false,

    val completedCount: Int = 0
) {
    /** Converts the 25 fixed columns into a generic "section.task" -> checked map used by the UI layer. */
    fun toTaskMap(): Map<String, Boolean> = mapOf(
        "preLecture.physics" to preLecturePhysics,
        "preLecture.chemistry" to preLectureChemistry,
        "preLecture.biology" to preLectureBiology,
        "lectures.physics" to lecturesPhysics,
        "lectures.chemistry" to lecturesChemistry,
        "lectures.biology" to lecturesBiology,
        "lectures.extra" to lecturesExtra,
        "notes.physics" to notesPhysics,
        "notes.chemistry" to notesChemistry,
        "notes.biology" to notesBiology,
        "dpp.physics" to dppPhysics,
        "dpp.chemistry" to dppChemistry,
        "dpp.biology" to dppBiology,
        "assignments.physics" to assignmentsPhysics,
        "assignments.chemistry" to assignmentsChemistry,
        "assignments.biology" to assignmentsBiology,
        "postLecture.physics" to postLecturePhysics,
        "postLecture.chemistry" to postLectureChemistry,
        "postLecture.biology" to postLectureBiology,
        "practice.physics" to practicePhysics,
        "practice.chemistry" to practiceChemistry,
        "practice.biology" to practiceBiology,
        "ncert.reading" to ncertReading,
        "tests.test" to testsTest,
        "tests.analysis" to testsAnalysis
    )

    companion object {
        /** Builds a row from a generic "section.task" -> checked map (missing keys default to false). */
        fun fromTaskMap(date: String, checked: Map<String, Boolean>): DailyProgress {
            fun v(id: String) = checked[id] ?: false
            return DailyProgress(
                date = date,
                preLecturePhysics = v("preLecture.physics"),
                preLectureChemistry = v("preLecture.chemistry"),
                preLectureBiology = v("preLecture.biology"),
                lecturesPhysics = v("lectures.physics"),
                lecturesChemistry = v("lectures.chemistry"),
                lecturesBiology = v("lectures.biology"),
                lecturesExtra = v("lectures.extra"),
                notesPhysics = v("notes.physics"),
                notesChemistry = v("notes.chemistry"),
                notesBiology = v("notes.biology"),
                dppPhysics = v("dpp.physics"),
                dppChemistry = v("dpp.chemistry"),
                dppBiology = v("dpp.biology"),
                assignmentsPhysics = v("assignments.physics"),
                assignmentsChemistry = v("assignments.chemistry"),
                assignmentsBiology = v("assignments.biology"),
                postLecturePhysics = v("postLecture.physics"),
                postLectureChemistry = v("postLecture.chemistry"),
                postLectureBiology = v("postLecture.biology"),
                practicePhysics = v("practice.physics"),
                practiceChemistry = v("practice.chemistry"),
                practiceBiology = v("practice.biology"),
                ncertReading = v("ncert.reading"),
                testsTest = v("tests.test"),
                testsAnalysis = v("tests.analysis"),
                completedCount = checked.values.count { it }
            )
        }
    }
}

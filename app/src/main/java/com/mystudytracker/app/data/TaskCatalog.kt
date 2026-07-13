package com.mystudytracker.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mystudytracker.app.util.DateRules
import java.time.LocalDate

/** A single checkbox row inside a section, e.g. "Physics" inside "Lectures". */
data class TaskItem(val key: String, val label: String)

/** One of the 9 fixed sections shown on the daily checklist screen. */
data class SectionDefinition(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val tasks: List<TaskItem>,
    // Optional dynamic scope text shown as a badge under the section title, e.g. "Previous 2 Lectures".
    val ruleProvider: ((LocalDate) -> String)? = null
)

/**
 * The fixed catalog of sections/tasks for NEET 2027 prep, shown in this exact order on the
 * checklist screen. This list is the single source of truth for the checklist UI; the Room
 * entity mapping in [DailyProgress] (columns + toTaskMap/fromTaskMap) must stay in sync with it -
 * adding, removing, or renaming a task key here requires a matching schema migration.
 */
object TaskCatalog {

    val sections: List<SectionDefinition> = listOf(
        SectionDefinition(
            key = "preLecture",
            title = "Pre-Lecture Revision",
            icon = Icons.Outlined.WbSunny,
            iconTint = Color(0xFFFBBF24),
            tasks = listOf(TaskItem("physics", "Physics"), TaskItem("chemistry", "Chemistry"), TaskItem("biology", "Biology")),
            ruleProvider = { date -> DateRules.preLectureRule(date) }
        ),
        SectionDefinition(
            key = "lectures",
            title = "Lectures",
            icon = Icons.Outlined.Videocam,
            iconTint = Color(0xFFFB7185),
            tasks = listOf(
                TaskItem("physics", "Physics"),
                TaskItem("chemistry", "Chemistry"),
                TaskItem("biology", "Biology"),
                TaskItem("extra", "Extra")
            )
        ),
        SectionDefinition(
            key = "notes",
            title = "Notes",
            icon = Icons.Outlined.EditNote,
            iconTint = Color(0xFF38BDF8),
            tasks = listOf(TaskItem("physics", "Physics"), TaskItem("chemistry", "Chemistry"), TaskItem("biology", "Biology"))
        ),
        SectionDefinition(
            key = "dpp",
            title = "DPP",
            icon = Icons.Outlined.Description,
            iconTint = Color(0xFFA78BFA),
            tasks = listOf(TaskItem("physics", "Physics"), TaskItem("chemistry", "Chemistry"), TaskItem("biology", "Biology"))
        ),
        SectionDefinition(
            key = "assignments",
            title = "Assignments",
            icon = Icons.AutoMirrored.Outlined.Assignment,
            iconTint = Color(0xFF2DD4BF),
            tasks = listOf(TaskItem("physics", "Physics"), TaskItem("chemistry", "Chemistry"), TaskItem("biology", "Biology"))
        ),
        SectionDefinition(
            key = "postLecture",
            title = "Post-Lecture Revision",
            icon = Icons.Outlined.NightsStay,
            iconTint = Color(0xFF818CF8),
            tasks = listOf(TaskItem("physics", "Physics"), TaskItem("chemistry", "Chemistry"), TaskItem("biology", "Biology")),
            ruleProvider = { "Today's Lecture" }
        ),
        SectionDefinition(
            key = "homework",
            title = "Homework",
            icon = Icons.Outlined.Home,
            iconTint = Color(0xFFF472B6),
            tasks = listOf(TaskItem("physics", "Physics"), TaskItem("chemistry", "Chemistry"), TaskItem("biology", "Biology"))
        ),
        SectionDefinition(
            key = "practice",
            title = "Practice",
            icon = Icons.Outlined.TrackChanges,
            iconTint = Color(0xFFFB923C),
            tasks = listOf(TaskItem("physics", "Physics"), TaskItem("chemistry", "Chemistry"), TaskItem("biology", "Biology")),
            ruleProvider = { date -> DateRules.practiceRule(date) }
        ),
        SectionDefinition(
            key = "ncert",
            title = "Reading",
            icon = Icons.Outlined.MenuBook,
            iconTint = Color(0xFFA3E635),
            tasks = listOf(TaskItem("reading", "NCERT Reading")),
            // Same scope rule as Practice: current chapter on weekdays, current + previous chapter
            // on alternate Sundays, all previous chapters covered on the other Sundays.
            ruleProvider = { date -> DateRules.practiceRule(date) }
        ),
        SectionDefinition(
            key = "tests",
            title = "Tests",
            icon = Icons.Outlined.FactCheck,
            iconTint = Color(0xFFE879F9),
            tasks = listOf(TaskItem("test", "Test"), TaskItem("analysis", "Test Analysis"))
        )
    )

    /** One checkbox per NEET prep task, every day - derived from [sections] so it always matches. */
    val totalTaskCount: Int = sections.sumOf { it.tasks.size }
}

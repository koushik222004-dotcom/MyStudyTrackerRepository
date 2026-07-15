package com.mystudytracker.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Category
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

/**
 * A node in a section's task tree. The tree can nest to any depth - a [TaskLeaf] is an actual
 * checkable/quantity-trackable task; a [TaskGroup] is a purely organizational parent (e.g.
 * "Chemistry" grouping Physical/Organic/Inorganic) with no state of its own. Every parent's
 * on-screen state (checked/partial/empty, N/A) is always *derived* from its leaf descendants -
 * see [TaskCatalog.leafKeysUnder] - never stored independently. This keeps backlog and progress
 * math unambiguous: only leaves are ever counted.
 */
sealed interface CatalogNode {
    val key: String
    val title: String
}

/** An actual trackable task - supports done/pending, an optional quantity > 1, and "Not Applicable". */
data class TaskLeaf(
    override val key: String,
    override val title: String
) : CatalogNode

/** A purely organizational grouping of child nodes. Long-press cascades "Not Applicable" to every
 *  leaf underneath; tap cascades check/uncheck via the checkbox. Never has its own stored state. */
data class TaskGroup(
    override val key: String,
    override val title: String,
    val children: List<CatalogNode>
) : CatalogNode

/** One of the fixed top-level sections shown on the checklist screen. */
data class SectionDefinition(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val children: List<CatalogNode>,
    // Optional dynamic scope text shown as a badge under the section title, e.g. "Previous 2 Lectures".
    val ruleProvider: ((LocalDate) -> String)? = null
)

/**
 * The fixed catalog of sections/tasks for NEET 2027 prep, shown in this exact order on the
 * checklist screen. This is the single source of truth for the checklist UI *and* for every full
 * task key stored in [DailyTaskState] (a full key is the dot-joined path of node keys from section
 * down to leaf, e.g. "lectures.chemistry.organic" or "tests.test.full.physicalChemistry").
 *
 * Because per-day state lives in a normalized table keyed by these path strings (see
 * [DailyTaskState]) rather than one fixed Room column per task, changing this catalog - adding,
 * renaming, or re-nesting a task - never requires a schema migration. Only genuinely renaming a
 * leaf's key (not its title) invalidates already-saved history for that task, since the key *is*
 * the durable identity used for storage and backlog aggregation.
 */
object TaskCatalog {

    /** The 6 subject leaves shared by every "regular" section: Physics, Chemistry (Physical /
     *  Organic / Inorganic), Biology (Botany / Zoology). */
    private fun subjectChildren(): List<CatalogNode> = listOf(
        TaskLeaf("physics", "Physics"),
        TaskGroup(
            "chemistry", "Chemistry", listOf(
                TaskLeaf("physical", "Physical Chemistry"),
                TaskLeaf("organic", "Organic Chemistry"),
                TaskLeaf("inorganic", "Inorganic Chemistry")
            )
        ),
        TaskGroup(
            "biology", "Biology", listOf(
                TaskLeaf("botany", "Botany"),
                TaskLeaf("zoology", "Zoology")
            )
        )
    )

    /**
     * 8 subject leaves for Partial Syllabus Tests. Keys are stable identifiers — only the
     * displayed titles differ from a plain "Combined" label to the explicit subject list.
     */
    private fun testPartialLeaves(): List<CatalogNode> = listOf(
        TaskLeaf("physics", "Physics"),
        TaskLeaf("physicalChemistry", "Physical Chemistry"),
        TaskLeaf("organicChemistry", "Organic Chemistry"),
        TaskLeaf("inorganicChemistry", "Inorganic Chemistry"),
        TaskLeaf("chemistryCombined", "Chemistry (Physical + Organic + Inorganic)"),
        TaskLeaf("botany", "Botany"),
        TaskLeaf("zoology", "Zoology"),
        TaskLeaf("biologyCombined", "Biology (Botany + Zoology)")
    )

    /**
     * Full Syllabus Test leaves: same 8 as partial, plus the combined all-subjects leaf.
     */
    private fun testFullLeaves(): List<CatalogNode> = testPartialLeaves() + listOf(
        TaskLeaf("fullSyllabus", "Full Syllabus (Physics + Chemistry + Biology)")
    )

    /** Shared test subtree used by both Test and Test Analysis groups. */
    private fun testSubtree(groupKey: String, groupTitle: String): TaskGroup = TaskGroup(
        key = groupKey,
        title = groupTitle,
        children = listOf(
            TaskGroup("partial", "Partial Syllabus Test", testPartialLeaves()),
            TaskGroup("full", "Full Syllabus Test", testFullLeaves())
        )
    )

    val sections: List<SectionDefinition> = listOf(
        SectionDefinition(
            key = "preLecture",
            title = "Pre-Lecture Revision",
            icon = Icons.Outlined.WbSunny,
            iconTint = Color(0xFFFBBF24),
            children = subjectChildren(),
            ruleProvider = { date -> DateRules.preLectureRule(date) }
        ),
        SectionDefinition(
            key = "lectures",
            title = "Lectures",
            icon = Icons.Outlined.Videocam,
            iconTint = Color(0xFFFB7185),
            children = subjectChildren()
        ),
        SectionDefinition(
            key = "notes",
            title = "Notes",
            icon = Icons.Outlined.EditNote,
            iconTint = Color(0xFF38BDF8),
            children = subjectChildren()
        ),
        SectionDefinition(
            key = "dpp",
            title = "DPP",
            icon = Icons.Outlined.Description,
            iconTint = Color(0xFFA78BFA),
            children = subjectChildren()
        ),
        SectionDefinition(
            key = "assignments",
            title = "Assignments",
            icon = Icons.AutoMirrored.Outlined.Assignment,
            iconTint = Color(0xFF2DD4BF),
            children = subjectChildren()
        ),
        SectionDefinition(
            key = "postLecture",
            title = "Post-Lecture Revision",
            icon = Icons.Outlined.NightsStay,
            iconTint = Color(0xFF818CF8),
            children = subjectChildren()
            // No ruleProvider — Post-Lecture Revision always covers today's lecture,
            // so a fixed label adds no information and is omitted like Lectures and DPP.
        ),
        SectionDefinition(
            key = "homework",
            title = "Homework",
            icon = Icons.Outlined.Home,
            iconTint = Color(0xFFF472B6),
            children = subjectChildren()
        ),
        SectionDefinition(
            key = "practice",
            title = "Question Practice",
            icon = Icons.Outlined.TrackChanges,
            iconTint = Color(0xFFFB923C),
            children = subjectChildren(),
            ruleProvider = { date -> DateRules.practiceRule(date) }
        ),
        SectionDefinition(
            key = "reading",
            title = "NCERT Reading",
            icon = Icons.Outlined.MenuBook,
            iconTint = Color(0xFFA3E635),
            children = subjectChildren(),
            ruleProvider = { date -> DateRules.practiceRule(date) }
        ),
        SectionDefinition(
            key = "tests",
            title = "Tests",
            icon = Icons.Outlined.FactCheck,
            iconTint = Color(0xFFE879F9),
            children = listOf(
                testSubtree("test", "Test"),
                testSubtree("analysis", "Test Analysis")
            )
        ),
        SectionDefinition(
            key = "misc",
            title = "Miscellaneous",
            icon = Icons.Outlined.Category,
            iconTint = Color(0xFF67E8F9),
            children = subjectChildren()
        )
    )

    /** Every leaf's full dot-joined key, e.g. "tests.test.full.physicalChemistry". */
    val allLeafKeys: List<String> by lazy {
        sections.flatMap { section -> leafKeysUnder(section.children, section.key) }
    }

    /** Total number of leaf tasks in the whole catalog - the denominator for a "fully caught up" day. */
    val totalLeafCount: Int by lazy { allLeafKeys.size }

    /** Recursively collects full leaf keys under a list of sibling nodes, prefixed by [pathPrefix]. */
    fun leafKeysUnder(nodes: List<CatalogNode>, pathPrefix: String): List<String> =
        nodes.flatMap { node -> leafKeysUnder(node, pathPrefix) }

    /** Recursively collects full leaf keys under a single node (itself if it's a leaf), prefixed by [pathPrefix]. */
    fun leafKeysUnder(node: CatalogNode, pathPrefix: String): List<String> {
        val path = "$pathPrefix.${node.key}"
        return when (node) {
            is TaskLeaf -> listOf(path)
            is TaskGroup -> node.children.flatMap { child -> leafKeysUnder(child, path) }
        }
    }

    /** Human-readable path of titles from section down to leaf, e.g. ["Lectures", "Chemistry", "Organic Chemistry"]. */
    fun titlePathFor(fullKey: String): List<String> {
        val segments = fullKey.split(".")
        val section = sections.firstOrNull { it.key == segments.getOrNull(0) } ?: return emptyList()
        val path = mutableListOf(section.title)
        var nodes: List<CatalogNode> = section.children
        for (i in 1 until segments.size) {
            val node = nodes.firstOrNull { it.key == segments[i] } ?: break
            path.add(node.title)
            nodes = (node as? TaskGroup)?.children ?: emptyList()
        }
        return path
    }
}

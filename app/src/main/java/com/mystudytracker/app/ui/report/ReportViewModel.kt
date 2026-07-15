package com.mystudytracker.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.ProgressRepository
import com.mystudytracker.app.data.TaskCatalog
import com.mystudytracker.app.util.DateIntegrityManager
import com.mystudytracker.app.util.DateRules
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One leaf's backlog: how many units are pending across the whole tracked range, and its display path. */
data class LeafBacklog(val taskKey: String, val titlePath: List<String>, val pendingUnits: Int)

/** One section's or group's rolled-up subtotal, containing either nested subtotals or leaves. */
data class BacklogNode(
    val title: String,
    val pendingUnits: Int,
    val children: List<BacklogNode>,
    val leaf: LeafBacklog?
)

class ReportViewModel(
    private val repository: ProgressRepository,
    dateIntegrityManager: DateIntegrityManager
) : ViewModel() {

    private val _tree = MutableStateFlow<List<BacklogNode>>(emptyList())
    val tree: StateFlow<List<BacklogNode>> = _tree.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selectedTask = MutableStateFlow<LeafBacklog?>(null)
    val selectedTask: StateFlow<LeafBacklog?> = _selectedTask.asStateFlow()

    private val _pendingDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val pendingDates: StateFlow<List<LocalDate>> = _pendingDates.asStateFlow()

    private val today: LocalDate = dateIntegrityManager.currentState().today ?: LocalDate.now()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val throughDate = today.toString()
            // Every tracked day up to and including today counts toward "untouched day = pending",
            // matching the calendar's convention that a day with no saved row is fully outstanding.
            val trackedDayCount = (ChronoUnit.DAYS.between(DateRules.START_DATE, today) + 1)
                .coerceAtLeast(0)
                .toInt()
            val pendingByLeaf = repository.backlogByLeaf(throughDate, trackedDayCount)
            _tree.value = TaskCatalog.sections.map { section -> buildSectionNode(section.title, section.key, section.children, pendingByLeaf) }
            _loading.value = false
        }
    }

    private fun buildSectionNode(
        title: String,
        pathPrefix: String,
        children: List<com.mystudytracker.app.data.CatalogNode>,
        pendingByLeaf: Map<String, Int>
    ): BacklogNode {
        val childNodes = children.map { child -> buildNode(child, pathPrefix, pendingByLeaf) }
        return BacklogNode(title = title, pendingUnits = childNodes.sumOf { it.pendingUnits }, children = childNodes, leaf = null)
    }

    private fun buildNode(
        node: com.mystudytracker.app.data.CatalogNode,
        pathPrefix: String,
        pendingByLeaf: Map<String, Int>
    ): BacklogNode {
        val fullKey = "$pathPrefix.${node.key}"
        return when (node) {
            is com.mystudytracker.app.data.TaskLeaf -> {
                val pending = pendingByLeaf[fullKey] ?: 0
                BacklogNode(
                    title = node.title,
                    pendingUnits = pending,
                    children = emptyList(),
                    leaf = LeafBacklog(fullKey, TaskCatalog.titlePathFor(fullKey), pending)
                )
            }
            is com.mystudytracker.app.data.TaskGroup -> {
                val childNodes = node.children.map { child -> buildNode(child, fullKey, pendingByLeaf) }
                BacklogNode(title = node.title, pendingUnits = childNodes.sumOf { it.pendingUnits }, children = childNodes, leaf = null)
            }
        }
    }

    /** Opens the drill-down list of specific dates a leaf is still outstanding on. */
    fun selectTask(leaf: LeafBacklog) {
        _selectedTask.value = leaf
        viewModelScope.launch {
            _pendingDates.value = repository.pendingDatesForTask(leaf.taskKey, today.toString())
                .map { LocalDate.parse(it) }
        }
    }

    fun clearSelection() {
        _selectedTask.value = null
        _pendingDates.value = emptyList()
    }

    companion object {
        fun factory(repository: ProgressRepository, dateIntegrityManager: DateIntegrityManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReportViewModel(repository, dateIntegrityManager) as T
            }
    }
}

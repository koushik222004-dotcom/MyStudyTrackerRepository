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

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Today as confirmed by the date-integrity system. Null means no sync has happened yet.
    // Never falls back to LocalDate.now() - the backlog must only count days the user has
    // explicitly confirmed, so an unsynced device shows a "sync required" prompt instead of
    // silently computing backlog against the phone's wall clock.
    private val _today = MutableStateFlow<LocalDate?>(dateIntegrityManager.currentState().today)
    val today: StateFlow<LocalDate?> = _today.asStateFlow()

    init {
        if (_today.value != null) refresh()
    }

    private fun refresh() {
        val today = _today.value ?: return
        viewModelScope.launch {
            _loading.value = true

            // Backlog counts only past days — today is still "in progress" so it is excluded.
            // throughDate is yesterday; trackedDayCount is the number of days from START_DATE
            // up to and including yesterday (i.e. DAYS.between(START, today) with no +1).
            val yesterday = today.minusDays(1)
            if (yesterday.isBefore(DateRules.START_DATE)) {
                // Edge case: synced on the very first day — no past days to report backlog for.
                _tree.value = emptyList()
                _loading.value = false
                return@launch
            }

            val throughDate = yesterday.toString()
            val trackedDayCount = ChronoUnit.DAYS.between(DateRules.START_DATE, today)
                .coerceAtLeast(0)
                .toInt()

            val pendingByLeaf = repository.backlogByLeaf(throughDate, trackedDayCount)
            _tree.value = TaskCatalog.sections.map { section ->
                buildSectionNode(section.title, section.key, section.children, pendingByLeaf)
            }
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

    companion object {
        fun factory(repository: ProgressRepository, dateIntegrityManager: DateIntegrityManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReportViewModel(repository, dateIntegrityManager) as T
            }
    }
}

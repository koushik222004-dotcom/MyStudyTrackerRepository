package com.mystudytracker.app.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.AttachmentType
import com.mystudytracker.app.data.DailyAttachment
import com.mystudytracker.app.data.DailyProgress
import com.mystudytracker.app.data.DailyTaskState
import com.mystudytracker.app.data.ProgressRepository
import com.mystudytracker.app.data.TaskCatalog
import com.mystudytracker.app.data.isDone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChecklistViewModel(
    private val repository: ProgressRepository,
    private val date: String
) : ViewModel() {

    // Single shared observation of this date's row - locked/note/unit totals all derive from it,
    // so only one Room query is open for the same row at a time.
    private val day: StateFlow<DailyProgress?> = repository.observeByDate(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Once true, this day is permanently finalized - there is no way back to false. */
    val locked: StateFlow<Boolean> = day
        .map { it?.locked ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Free-form remark for this day. Editable independently of lock status. */
    val note: StateFlow<String?> = day
        .map { it?.note }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Denormalized completed/total unit counts for this day - see ProgressRepository.recomputeAggregate. */
    val completedUnits: StateFlow<Int> = day
        .map { it?.completedUnits ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalUnits: StateFlow<Int> = day
        .map { it?.totalUnits ?: TaskCatalog.totalLeafCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskCatalog.totalLeafCount)

    /** File attachments for this day, ordered by insertion time. */
    val attachments: StateFlow<List<DailyAttachment>> = repository.observeAttachments(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rows actually committed to Room, keyed by full task path. A leaf with no entry here is
    // implicitly "pending, quantity 1, applies" - see DailyTaskState.default.
    private val committedRows: StateFlow<Map<String, DailyTaskState>> = repository.observeTaskStates(date)
        .map { list -> list.associateBy { it.taskKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Optimistic overlay for taps/long-presses that haven't round-tripped through Room's Flow
    // yet - same rationale as the old single-map `pendingChecked`: two fast taps on the same or
    // different tasks must each build on the *result* of the previous one, not both read the same
    // stale committed value and silently discard one another's change. An override for a key is
    // dropped once [committedRows] catches up and matches it exactly (see init below).
    private val overrides = MutableStateFlow<Map<String, DailyTaskState>>(emptyMap())

    /** Effective per-leaf state for this day: committed rows with any in-flight taps applied on top. */
    val taskStates: StateFlow<Map<String, DailyTaskState>> = combine(committedRows, overrides) { committed, over ->
        committed + over
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            committedRows.collect { committed ->
                overrides.update { current -> current.filterNot { (key, value) -> committed[key] == value } }
            }
        }
    }

    private fun currentOf(key: String): DailyTaskState? = taskStates.value[key]

    /** Taps a single leaf: plain done/undone flip if its quantity is 1, otherwise cycles 0..target..0. No-op while excluded. */
    fun toggleLeaf(key: String) {
        if (locked.value) return
        val current = currentOf(key)
        if (current?.notApplicable == true) return
        val target = current?.targetCount ?: 1
        val completed = current?.completedCount ?: 0
        val next = if (completed >= target) 0 else completed + 1
        overrides.update { it + (key to DailyTaskState(date, key, next, target, false)) }
        viewModelScope.launch { repository.toggleLeaf(date, key, current) }
    }

    /** Sets an explicit quantity for a leaf via the long-press menu's stepper. */
    fun setQuantity(key: String, newTarget: Int) {
        if (locked.value) return
        val current = currentOf(key)
        val target = newTarget.coerceAtLeast(1)
        val completed = (current?.completedCount ?: 0).coerceAtMost(target)
        overrides.update { it + (key to DailyTaskState(date, key, completed, target, current?.notApplicable ?: false)) }
        viewModelScope.launch { repository.setLeafQuantity(date, key, current, target) }
    }

    /** Toggles a single leaf's "doesn't apply" flag from the long-press menu. */
    fun setNotApplicable(key: String, notApplicable: Boolean) {
        if (locked.value) return
        val current = currentOf(key)
        overrides.update {
            it + (key to DailyTaskState(date, key, current?.completedCount ?: 0, current?.targetCount ?: 1, notApplicable))
        }
        viewModelScope.launch { repository.setLeafNotApplicable(date, key, current, notApplicable) }
    }

    /** Tapping a section/group header: completes every descendant leaf, or resets all to zero if all are already done. */
    fun toggleGroup(leafKeys: List<String>) {
        if (locked.value) return
        val currentByKey = leafKeys.associateWith { currentOf(it) }
        val allDone = leafKeys.all { currentByKey[it].isDone() }
        val overrideRows = leafKeys.associateWith { key ->
            val existing = currentByKey[key]
            val target = existing?.targetCount ?: 1
            DailyTaskState(date, key, if (allDone) 0 else target, target, false)
        }
        overrides.update { it + overrideRows }
        viewModelScope.launch { repository.toggleGroup(date, leafKeys, currentByKey) }
    }

    /** Long-pressing a section/group header: marks every descendant leaf "doesn't apply", or clears it if all are already excluded. */
    fun toggleGroupNotApplicable(leafKeys: List<String>) {
        if (locked.value) return
        val currentByKey = leafKeys.associateWith { currentOf(it) }
        val allExcluded = leafKeys.all { currentByKey[it]?.notApplicable == true }
        val overrideRows = leafKeys.associateWith { key ->
            val existing = currentByKey[key]
            DailyTaskState(date, key, existing?.completedCount ?: 0, existing?.targetCount ?: 1, !allExcluded)
        }
        overrides.update { it + overrideRows }
        viewModelScope.launch { repository.toggleGroupNotApplicable(date, leafKeys, currentByKey) }
    }

    /** Permanently finalizes this day. Irreversible - there is no unlock path anywhere in the app. */
    fun lockDay() {
        if (locked.value) return
        viewModelScope.launch {
            repository.lockDay(date)
        }
    }

    /** Saves the day's remark. Allowed regardless of lock status - locking only freezes tasks. */
    fun saveNote(text: String) {
        viewModelScope.launch {
            repository.saveNote(date, text.ifBlank { null })
        }
    }

    /** Adds a file attachment after it has been copied to internal storage by the UI layer. */
    fun addAttachment(filePath: String, type: AttachmentType, displayName: String) {
        viewModelScope.launch {
            repository.addAttachment(
                DailyAttachment(date = date, filePath = filePath, type = type, displayName = displayName)
            )
        }
    }

    /** Removes the attachment record and deletes its backing file from internal storage. */
    fun removeAttachment(id: Long) {
        viewModelScope.launch {
            repository.removeAttachment(id)
        }
    }

    companion object {
        fun factory(repository: ProgressRepository, date: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChecklistViewModel(repository, date) as T
            }
    }
}

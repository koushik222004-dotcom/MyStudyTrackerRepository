package com.mystudytracker.app.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.DailyProgress
import com.mystudytracker.app.data.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChecklistViewModel(
    private val repository: ProgressRepository,
    private val date: String
) : ViewModel() {

    // Single shared observation of this date's row - checked/locked/note all derive from it below
    // instead of each opening their own independent Room query for the exact same row.
    private val day: StateFlow<DailyProgress?> = repository.observeByDate(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val checked: StateFlow<Map<String, Boolean>> = day
        .map { it?.toTaskMap() ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Once true, this day is permanently finalized - there is no way back to false. */
    val locked: StateFlow<Boolean> = day
        .map { it?.locked ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Free-form note for this day. Editable independently of lock status. */
    val note: StateFlow<String?> = day
        .map { it?.note }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggle(taskId: String) {
        if (locked.value) return
        val updated = checked.value.toMutableMap()
        updated[taskId] = !(updated[taskId] ?: false)
        viewModelScope.launch {
            repository.saveDay(date, updated)
        }
    }

    /** Permanently finalizes this day. Irreversible - there is no unlock path anywhere in the app. */
    fun lockDay() {
        if (locked.value) return
        viewModelScope.launch {
            repository.lockDay(date)
        }
    }

    /** Saves the day's note. Allowed regardless of lock status - locking only freezes tasks. */
    fun saveNote(text: String) {
        viewModelScope.launch {
            repository.saveNote(date, text.ifBlank { null })
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

package com.mystudytracker.app.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.AttachmentType
import com.mystudytracker.app.data.DailyAttachment
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

    // Single shared observation of this date's row - checked/locked/note all derive from it,
    // so only one Room query is open for the same row at a time.
    private val day: StateFlow<DailyProgress?> = repository.observeByDate(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val checked: StateFlow<Map<String, Boolean>> = day
        .map { it?.toTaskMap() ?: emptyMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Once true, this day is permanently finalized - there is no way back to false. */
    val locked: StateFlow<Boolean> = day
        .map { it?.locked ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Free-form remark for this day. Editable independently of lock status. */
    val note: StateFlow<String?> = day
        .map { it?.note }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** File attachments for this day, ordered by insertion time. */
    val attachments: StateFlow<List<DailyAttachment>> = repository.observeAttachments(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks the map most recently sent to the database, updated synchronously inside [toggle].
    // [checked] only reflects a write once Room's Flow round-trips it back, which - while
    // normally near-instant - is not instantaneous: two taps close enough together would
    // otherwise both read the same stale `checked.value` as their base, so the second write could
    // silently discard the first tap's change. Basing each new toggle on this in-memory map
    // instead means every write always builds on every prior write, regardless of how fast Room's
    // Flow keeps up.
    private var pendingChecked: Map<String, Boolean>? = null

    fun toggle(taskId: String) {
        if (locked.value) return
        val base = pendingChecked ?: checked.value
        val updated = base.toMutableMap()
        updated[taskId] = !(updated[taskId] ?: false)
        pendingChecked = updated
        viewModelScope.launch {
            repository.saveDay(date, updated, locked = locked.value, note = note.value)
        }
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

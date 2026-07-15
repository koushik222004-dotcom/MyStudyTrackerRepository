package com.mystudytracker.app.ui.calendar

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.DailyProgress
import com.mystudytracker.app.data.ProgressRepository
import com.mystudytracker.app.util.DateIntegrityManager
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CalendarViewModel(
    repository: ProgressRepository,
    private val dateIntegrityManager: DateIntegrityManager
) : ViewModel() {

    /** ISO date string -> that day's completed/total unit row, for every day with any saved progress. */
    val progressByDate: StateFlow<Map<String, DailyProgress>> = repository.observeAll()
        .map { rows -> rows.associateBy { it.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // "Today" as computed by the uptime-anchored date-integrity system - null only before the very
    // first sync has ever happened. Never derived from the phone's wall clock.
    private val _today = MutableStateFlow<LocalDate?>(null)
    val today: StateFlow<LocalDate?> = _today.asStateFlow()

    private val _lastSyncedLabel = MutableStateFlow<String?>(null)
    val lastSyncedLabel: StateFlow<String?> = _lastSyncedLabel.asStateFlow()

    // True when the device rebooted since the last confirmed date, so the tracked "today" can no
    // longer be trusted until the user re-syncs. Surfaced as an attention state on the sync pill
    // itself rather than a separate banner, so nothing else on screen shifts when it appears.
    private val _rebootDetected = MutableStateFlow(false)
    val rebootDetected: StateFlow<Boolean> = _rebootDetected.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    // Briefly true right after a successful sync, so the sync pill can flash a checkmark before
    // reverting to its normal icon.
    private val _justSynced = MutableStateFlow(false)
    val justSynced: StateFlow<Boolean> = _justSynced.asStateFlow()

    // Briefly true right after a failed sync attempt, so the sync pill can flash an attention
    // state before reverting to its normal label.
    private val _syncFailed = MutableStateFlow(false)
    val syncFailed: StateFlow<Boolean> = _syncFailed.asStateFlow()

    init {
        refreshFromClock()
    }

    /** Cheap, no-network recompute of "today" from anchor + elapsed uptime. Safe to call anytime. */
    private fun refreshFromClock() {
        val state = dateIntegrityManager.currentState()
        _today.value = state.today
        _lastSyncedLabel.value = state.lastSyncedLabel
        _rebootDetected.value = state.rebootDetected
    }

    /** Explicit, user-triggered sync only. Never called automatically. */
    fun syncDate() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            _syncFailed.value = false
            val startedAt = SystemClock.elapsedRealtime()
            val result = dateIntegrityManager.syncNow()
            // With no network at all, DNS resolution fails almost instantly, so the request can
            // come back in a handful of milliseconds - too fast to read as a genuine attempt, and
            // indistinguishable from never having tried. Pad out to a minimum visible duration so
            // the spinner always shows a real attempt before reporting success or failure.
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (elapsed < MIN_SYNC_DISPLAY_MS) {
                delay(MIN_SYNC_DISPLAY_MS - elapsed)
            }
            // Clear the "syncing" flag as soon as the result is known, before showing the
            // follow-up success/failure indicator. Previously this stayed true through the whole
            // follow-up delay below, so the "syncing" branch in the UI's label/icon logic took
            // priority the entire time and completely masked both the success checkmark and the
            // failure warning - a failed sync looked identical to a successful one.
            _syncing.value = false
            when (result) {
                is DateIntegrityManager.SyncResult.Success -> {
                    _today.value = result.date
                    _lastSyncedLabel.value = result.label
                    _rebootDetected.value = false
                    _justSynced.value = true
                    delay(1500)
                    _justSynced.value = false
                }
                DateIntegrityManager.SyncResult.Failure -> {
                    refreshFromClock()
                    _syncFailed.value = true
                    delay(2500)
                    _syncFailed.value = false
                }
            }
        }
    }

    companion object {
        private const val MIN_SYNC_DISPLAY_MS = 900L

        fun factory(repository: ProgressRepository, appContext: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CalendarViewModel(repository, DateIntegrityManager(appContext)) as T
            }
    }
}

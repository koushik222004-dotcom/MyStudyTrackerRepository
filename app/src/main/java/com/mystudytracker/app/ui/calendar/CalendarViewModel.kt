package com.mystudytracker.app.ui.calendar

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.DailyProgress
import com.mystudytracker.app.data.ProgressRepository
import com.mystudytracker.app.util.DateIntegrityManager
import com.mystudytracker.app.util.DateRules
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val repository: ProgressRepository,
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

    // Total pending backlog units across all tasks, computed through yesterday (today excluded).
    // Null when no date has been synced yet. Used by the Backlog Report button to show a
    // "No Backlogs ✓" green state when the user is fully caught up.
    private val _totalBacklogUnits = MutableStateFlow<Int?>(null)
    val totalBacklogUnits: StateFlow<Int?> = _totalBacklogUnits.asStateFlow()

    init {
        refreshFromClock()
        // Recompute backlog total whenever the confirmed date changes (after sync or on first init).
        viewModelScope.launch {
            _today.collect { today -> recomputeBacklog(today) }
        }
    }

    /** Cheap, no-network recompute of "today" from anchor + elapsed uptime. Safe to call anytime. */
    private fun refreshFromClock() {
        val state = dateIntegrityManager.currentState()
        _today.value = state.today
        _lastSyncedLabel.value = state.lastSyncedLabel
        _rebootDetected.value = state.rebootDetected
    }

    /**
     * Computes the total pending backlog units through yesterday (today is excluded — it is still
     * "in progress"). Sets [_totalBacklogUnits] to null when no date is confirmed, or 0 when fully
     * caught up, or the actual sum otherwise.
     */
    private suspend fun recomputeBacklog(today: LocalDate?) {
        if (today == null) {
            _totalBacklogUnits.value = null
            return
        }
        val yesterday = today.minusDays(1)
        if (yesterday.isBefore(DateRules.START_DATE)) {
            _totalBacklogUnits.value = 0
            return
        }
        val throughDate = yesterday.toString()
        val trackedDayCount = ChronoUnit.DAYS.between(DateRules.START_DATE, today)
            .coerceAtLeast(0)
            .toInt()
        val pendingByLeaf = repository.backlogByLeaf(throughDate, trackedDayCount)
        _totalBacklogUnits.value = pendingByLeaf.values.sum()
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
            // follow-up success/failure indicator.
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

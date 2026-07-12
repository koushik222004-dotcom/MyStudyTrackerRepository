package com.mystudytracker.app.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.ProgressRepository
import com.mystudytracker.app.util.DateIntegrityManager
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarViewModel(
    repository: ProgressRepository,
    private val dateIntegrityManager: DateIntegrityManager
) : ViewModel() {

    /** ISO date string -> completed task count (0..25), for every day with any saved progress. */
    val completedCountByDate: StateFlow<Map<String, Int>> = repository.observeAll()
        .map { rows -> rows.associate { it.date to it.completedCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // "Today" as computed by the uptime-anchored date-integrity system - null only before the very
    // first sync has ever happened. Never derived from the phone's wall clock.
    private val _today = MutableStateFlow<LocalDate?>(null)
    val today: StateFlow<LocalDate?> = _today.asStateFlow()

    private val _lastSyncedLabel = MutableStateFlow<String?>(null)
    val lastSyncedLabel: StateFlow<String?> = _lastSyncedLabel.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    init {
        refreshFromClock()
    }

    /** Cheap, no-network recompute of "today" from anchor + elapsed uptime. Safe to call anytime. */
    private fun refreshFromClock() {
        val state = dateIntegrityManager.currentState()
        _today.value = state.today
        _lastSyncedLabel.value = state.lastSyncedLabel
        when {
            state.needsFirstSync ->
                _bannerMessage.value = "Welcome! Tap \"Sync Date\" once to set up your tracker (needs internet this one time)."
            state.rebootDetected ->
                _bannerMessage.value = "Device restarted — tap Sync Date to confirm today."
        }
    }

    /** Explicit, user-triggered sync only. Never called automatically. */
    fun syncDate() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            when (val result = dateIntegrityManager.syncNow()) {
                is DateIntegrityManager.SyncResult.Success -> {
                    _today.value = result.date
                    _lastSyncedLabel.value = result.label
                    _bannerMessage.value = "Date synced \u2713 (${result.label})"
                }
                DateIntegrityManager.SyncResult.Failure -> {
                    refreshFromClock()
                    _bannerMessage.value = "No internet — using last synced date"
                }
            }
            _syncing.value = false
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    companion object {
        fun factory(repository: ProgressRepository, appContext: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CalendarViewModel(repository, DateIntegrityManager(appContext)) as T
            }
    }
}

package com.mystudytracker.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CalendarViewModel(repository: ProgressRepository) : ViewModel() {

    /** ISO date string -> completed task count (0..25), for every day with any saved progress. */
    val completedCountByDate: StateFlow<Map<String, Int>> = repository.observeAll()
        .map { rows -> rows.associate { it.date to it.completedCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    companion object {
        fun factory(repository: ProgressRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CalendarViewModel(repository) as T
            }
    }
}

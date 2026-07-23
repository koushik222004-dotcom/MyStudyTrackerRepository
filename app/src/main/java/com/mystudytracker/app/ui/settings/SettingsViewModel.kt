package com.mystudytracker.app.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystudytracker.app.data.DailyProgress
import com.mystudytracker.app.data.ProgressRepository
import com.mystudytracker.app.data.backup.BackupManager
import com.mystudytracker.app.data.backup.BackupResult
import com.mystudytracker.app.data.backup.RestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupUiState {
    object Idle          : BackupUiState()
    object InProgress    : BackupUiState()
    object BackupSuccess : BackupUiState()
    object RestoreSuccess: BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

class SettingsViewModel(
    application: Application,
    private val repository: ProgressRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mst_settings", Context.MODE_PRIVATE)

    /** All tracked days — used to compute live stats shown on the Settings screen. */
    val allProgress: StateFlow<List<DailyProgress>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    private val _lastBackupAt = MutableStateFlow<Long?>(
        prefs.getLong("last_backup_at", -1L).takeIf { it != -1L }
    )
    val lastBackupAt: StateFlow<Long?> = _lastBackupAt.asStateFlow()

    // ── Backup ────────────────────────────────────────────────────────────────

    fun performBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _backupState.value = BackupUiState.InProgress
            try {
                val payload = repository.getAllDataForBackup()
                when (val result = BackupManager.writeToUri(getApplication(), uri, payload)) {
                    is BackupResult.Success -> {
                        val now = System.currentTimeMillis()
                        prefs.edit().putLong("last_backup_at", now).apply()
                        _lastBackupAt.value = now
                        _backupState.value = BackupUiState.BackupSuccess
                    }
                    is BackupResult.Error -> _backupState.value = BackupUiState.Error(result.message)
                }
            } catch (e: Exception) {
                _backupState.value = BackupUiState.Error("Backup failed: ${e.message}")
            }
        }
    }

    // ── Restore ───────────────────────────────────────────────────────────────

    fun performRestore(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _backupState.value = BackupUiState.InProgress
            try {
                when (val result = BackupManager.readFromUri(getApplication(), uri)) {
                    is RestoreResult.Success -> {
                        repository.restoreFromBackup(result.payload)
                        _backupState.value = BackupUiState.RestoreSuccess
                    }
                    is RestoreResult.Error -> _backupState.value = BackupUiState.Error(result.message)
                }
            } catch (e: Exception) {
                _backupState.value = BackupUiState.Error("Restore failed: ${e.message}")
            }
        }
    }

    fun clearBackupState() { _backupState.value = BackupUiState.Idle }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(
            repository: ProgressRepository,
            applicationContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(applicationContext as Application, repository) as T
        }
    }
}

package com.mystudytracker.app.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystudytracker.app.BuildConfig
import com.mystudytracker.app.ui.theme.AccentAmber
import com.mystudytracker.app.ui.theme.AccentBlue
import com.mystudytracker.app.ui.theme.AccentEmerald
import com.mystudytracker.app.ui.theme.AccentRed
import com.mystudytracker.app.ui.theme.ZincBackground
import com.mystudytracker.app.ui.theme.ZincBorder
import com.mystudytracker.app.ui.theme.ZincSurface
import com.mystudytracker.app.ui.theme.ZincSurfaceVariant
import com.mystudytracker.app.ui.theme.ZincTextMuted
import com.mystudytracker.app.ui.theme.ZincTextPrimary
import com.mystudytracker.app.ui.theme.ZincTextSecondary
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()
    val snackbar       = remember { SnackbarHostState() }

    val allProgress  by viewModel.allProgress.collectAsState()
    val backupState  by viewModel.backupState.collectAsState()
    val lastBackupAt by viewModel.lastBackupAt.collectAsState()

    // ── Derived stats ─────────────────────────────────────────────────────────
    val daysTracked    = allProgress.size
    val totalCompleted = allProgress.sumOf { it.completedUnits }
    val trackingSince  = allProgress.minByOrNull { it.date }?.date

    // ── Restart dialog ────────────────────────────────────────────────────────
    var showRestartDialog by remember { mutableStateOf(false) }

    // ── State side-effects ────────────────────────────────────────────────────
    LaunchedEffect(backupState) {
        when (val s = backupState) {
            is BackupUiState.BackupSuccess -> {
                scope.launch { snackbar.showSnackbar("Backup saved successfully.") }
                viewModel.clearBackupState()
            }
            is BackupUiState.RestoreSuccess -> {
                showRestartDialog = true
                // don't clear — dialog is still open
            }
            is BackupUiState.Error -> {
                scope.launch { snackbar.showSnackbar(s.message) }
                viewModel.clearBackupState()
            }
            else -> Unit
        }
    }

    // ── SAF launchers ─────────────────────────────────────────────────────────
    val today = LocalDate.now().toString()
    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.performBackup(it) } }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.performRestore(it) } }

    // ── Root layout ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZincBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ZincTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Settings",
                    color = ZincTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            // ── Scrollable content ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // ── Section: Backup & Restore ─────────────────────────────────
                SectionLabel("Backup & Restore")
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ZincSurface)
                        .border(1.dp, ZincBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    // ── Backup sub-section ────────────────────────────────────
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Create Backup",
                                color = ZincTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = lastBackupAt?.let { "Last backed up ${formatTimestamp(it)}" }
                                ?: "No backup created yet",
                            color = ZincTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        ActionButton(
                            label = "Create Backup",
                            enabled = backupState !is BackupUiState.InProgress,
                            containerColor = AccentBlue,
                            onClick = { createBackupLauncher.launch("studytracker_$today.mstb") }
                        )
                    }

                    HorizontalDivider(color = ZincBorder.copy(alpha = 0.5f))

                    // ── Restore sub-section ───────────────────────────────────
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = ZincTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Restore from Backup",
                                color = ZincTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Import a .mstb file to restore your study data.",
                            color = ZincTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        // Warning banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentAmber.copy(alpha = 0.08f))
                                .border(1.dp, AccentAmber.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "This will permanently replace all current study data.",
                                color = AccentAmber,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        ActionButton(
                            label = "Restore from Backup",
                            enabled = backupState !is BackupUiState.InProgress,
                            containerColor = AccentRed.copy(alpha = 0.12f),
                            labelColor = AccentRed,
                            onClick = { openBackupLauncher.launch(arrayOf("*/*")) }
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Section: Your Data ────────────────────────────────────────
                SectionLabel("Your Data")
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ZincSurface)
                        .border(1.dp, ZincBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    StatRow(label = "Days Tracked", value = if (daysTracked > 0) "$daysTracked" else "—")
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ZincBorder.copy(alpha = 0.4f)
                    )
                    StatRow(label = "Tasks Completed", value = if (daysTracked > 0) "$totalCompleted" else "—")
                    if (trackingSince != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = ZincBorder.copy(alpha = 0.4f)
                        )
                        StatRow(label = "Tracking Since", value = formatDate(trackingSince))
                    }
                }

                Spacer(Modifier.height(40.dp))

                // ── App info ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = ZincTextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "My Study Tracker  ·  v${BuildConfig.VERSION_NAME}",
                        color = ZincTextMuted,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }

        // ── Loading overlay ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = backupState is BackupUiState.InProgress,
            enter = fadeIn(tween(160)),
            exit  = fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZincBackground.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.5.dp)
            }
        }

        // ── Snackbar ──────────────────────────────────────────────────────────
        SnackbarHost(
            hostState = snackbar,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) { data ->
            Snackbar(
                snackbarData    = data,
                containerColor  = ZincSurfaceVariant,
                contentColor    = ZincTextPrimary,
                shape           = RoundedCornerShape(12.dp)
            )
        }
    }

    // ── Restart dialog ────────────────────────────────────────────────────────
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { /* force explicit action */ },
            containerColor   = ZincSurface,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text       = "Restore Complete",
                    color      = ZincTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 17.sp
                )
            },
            text = {
                Text(
                    text     = "The app needs to restart to apply your restored data.",
                    color    = ZincTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = context.packageManager
                            .getLaunchIntentForPackage(context.packageName)
                            ?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        intent?.let { context.startActivity(it) }
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                ) {
                    Text("Restart Now", color = AccentEmerald, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text.uppercase(Locale.getDefault()),
        color      = ZincTextMuted,
        fontSize   = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        modifier   = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = label, color = ZincTextSecondary, fontSize = 14.sp)
        Text(text = value, color = ZincTextPrimary,   fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    containerColor: androidx.compose.ui.graphics.Color,
    labelColor: androidx.compose.ui.graphics.Color = ZincTextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.4f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = if (enabled) labelColor else labelColor.copy(alpha = 0.5f),
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Formatting helpers ─────────────────────────────────────────────────────────────────────────

private val TIMESTAMP_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault())
private val DATE_FMT      = DateTimeFormatter.ofPattern("MMM d, yyyy",           Locale.getDefault())

private fun formatTimestamp(epochMillis: Long): String {
    val instant  = Instant.ofEpochMilli(epochMillis)
    val zdt      = instant.atZone(ZoneId.systemDefault())
    return TIMESTAMP_FMT.format(zdt)
}

private fun formatDate(isoDate: String): String {
    return try {
        DATE_FMT.format(LocalDate.parse(isoDate))
    } catch (_: Exception) { isoDate }
}

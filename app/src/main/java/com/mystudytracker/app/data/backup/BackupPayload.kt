package com.mystudytracker.app.data.backup

import com.mystudytracker.app.data.AttachmentType
import com.mystudytracker.app.data.DailyAttachment
import com.mystudytracker.app.data.DailyProgress
import com.mystudytracker.app.data.DailyTaskState

/**
 * Root object serialised into the encrypted .mstb backup file. [version] allows future
 * schema evolution — a newer app can detect an old backup and migrate gracefully.
 */
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val dailyProgress: List<DailyProgressEntry>,
    val dailyTaskStates: List<DailyTaskStateEntry>,
    val dailyAttachments: List<DailyAttachmentEntry>
)

data class DailyProgressEntry(
    val date: String,
    val completedUnits: Int,
    val totalUnits: Int,
    val locked: Boolean,
    val note: String?
)

data class DailyTaskStateEntry(
    val date: String,
    val taskKey: String,
    val completedCount: Int,
    val targetCount: Int,
    val notApplicable: Boolean
)

data class DailyAttachmentEntry(
    val date: String,
    val filePath: String,
    val type: String,           // stored as enum name so Gson doesn't need type adapters
    val displayName: String,
    val addedAt: Long
)

// ── Conversion helpers ────────────────────────────────────────────────────────────────────────

fun DailyProgress.toBackupEntry() =
    DailyProgressEntry(date, completedUnits, totalUnits, locked, note)

fun DailyTaskState.toBackupEntry() =
    DailyTaskStateEntry(date, taskKey, completedCount, targetCount, notApplicable)

fun DailyAttachment.toBackupEntry() =
    // id is omitted — it's autoGenerate and will be reassigned on restore
    DailyAttachmentEntry(date, filePath, type.name, displayName, addedAt)

fun DailyProgressEntry.toDailyProgress() =
    DailyProgress(date, completedUnits, totalUnits, locked, note)

fun DailyTaskStateEntry.toDailyTaskState() =
    DailyTaskState(date, taskKey, completedCount, targetCount, notApplicable)

fun DailyAttachmentEntry.toDailyAttachment() =
    // id = 0 triggers Room's autoGenerate
    DailyAttachment(id = 0, date = date, filePath = filePath,
        type = AttachmentType.valueOf(type), displayName = displayName, addedAt = addedAt)

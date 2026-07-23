package com.mystudytracker.app.data.backup

import com.google.gson.annotations.SerializedName
import com.mystudytracker.app.data.AttachmentType
import com.mystudytracker.app.data.DailyAttachment
import com.mystudytracker.app.data.DailyProgress
import com.mystudytracker.app.data.DailyTaskState

/**
 * Root object serialised into the encrypted .mstb backup file. [version] allows future
 * schema evolution — a newer app can detect an old backup and migrate gracefully.
 *
 * Every field carries an explicit @SerializedName so Gson always uses the annotated string
 * as the JSON key, regardless of how R8/ProGuard renames the Kotlin symbol at compile time.
 * Without this, a minified release build silently renames fields (e.g. "date" → "a"),
 * Gson can't match them on the way back in, and it leaves them null — crashing Kotlin's
 * non-null intrinsics with the obfuscated "parameter <this> is null" error.
 */
data class BackupPayload(
    @SerializedName("version")          val version: Int = 1,
    @SerializedName("exportedAt")       val exportedAt: Long,
    @SerializedName("dailyProgress")    val dailyProgress: List<DailyProgressEntry>,
    @SerializedName("dailyTaskStates")  val dailyTaskStates: List<DailyTaskStateEntry>,
    @SerializedName("dailyAttachments") val dailyAttachments: List<DailyAttachmentEntry>
)

data class DailyProgressEntry(
    @SerializedName("date")           val date: String,
    @SerializedName("completedUnits") val completedUnits: Int,
    @SerializedName("totalUnits")     val totalUnits: Int,
    @SerializedName("locked")         val locked: Boolean,
    @SerializedName("note")           val note: String?
)

data class DailyTaskStateEntry(
    @SerializedName("date")           val date: String,
    @SerializedName("taskKey")        val taskKey: String,
    @SerializedName("completedCount") val completedCount: Int,
    @SerializedName("targetCount")    val targetCount: Int,
    @SerializedName("notApplicable")  val notApplicable: Boolean
)

data class DailyAttachmentEntry(
    @SerializedName("date")        val date: String,
    @SerializedName("filePath")    val filePath: String,
    @SerializedName("type")        val type: String,        // stored as enum name; @SerializedName keeps it stable
    @SerializedName("displayName") val displayName: String,
    @SerializedName("addedAt")     val addedAt: Long,
    /**
     * Base64-encoded raw file bytes, embedded at backup time so the file survives restore
     * across devices or after the original file has been deleted. Null for legacy backups
     * that pre-date this field — those fall back to the stored [filePath] at open time.
     * Cleared (set to null) after the bytes have been written to internal storage on restore,
     * so it is never persisted to the database.
     */
    @SerializedName("fileContent") val fileContent: String? = null
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

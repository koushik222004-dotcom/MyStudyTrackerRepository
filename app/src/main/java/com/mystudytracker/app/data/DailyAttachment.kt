package com.mystudytracker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AttachmentType { IMAGE, VIDEO, AUDIO, DOCUMENT }

/**
 * Stores a single file attached to a study day. Files are copied into app-internal storage when
 * added, so [filePath] is always a stable absolute path that never expires - unlike raw content://
 * URIs from the system picker which can become inaccessible after the session ends.
 */
@Entity(tableName = "daily_attachments")
data class DailyAttachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,          // ISO date string, e.g. "2026-07-12"
    val filePath: String,      // absolute path inside app's files directory
    val type: AttachmentType,
    val displayName: String,   // original filename shown in the UI
    val addedAt: Long = System.currentTimeMillis()
)

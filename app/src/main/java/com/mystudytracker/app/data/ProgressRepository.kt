package com.mystudytracker.app.data

import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * Thin data-access layer between the ViewModels and Room - keeps DAO/SQL details out of the UI
 * layer. Owns both [DailyProgressDao] (task state, lock, remark) and [DailyAttachmentDao]
 * (per-day file attachments).
 */
class ProgressRepository(
    private val dao: DailyProgressDao,
    private val attachmentDao: DailyAttachmentDao
) {

    fun observeAll(): Flow<List<DailyProgress>> = dao.observeAll()

    fun observeByDate(date: String): Flow<DailyProgress?> = dao.observeByDate(date)

    suspend fun saveDay(date: String, checked: Map<String, Boolean>, locked: Boolean, note: String?) {
        dao.upsert(DailyProgress.fromTaskMap(date, checked, locked = locked, note = note))
    }

    /** Permanently finalizes a day. There is no corresponding "unlock" - this cannot be undone. */
    suspend fun lockDay(date: String) {
        dao.setLocked(date)
    }

    /**
     * Saves the free-form remark for [date], independent of task state and lock status. Creates a
     * fresh row (all tasks unchecked) if [date] has no saved progress yet.
     */
    suspend fun saveNote(date: String, note: String?) {
        val rowsUpdated = dao.updateNote(date, note)
        if (rowsUpdated == 0) {
            dao.upsert(DailyProgress(date = date, note = note))
        }
    }

    // ── Attachments ────────────────────────────────────────────────────────────────

    fun observeAttachments(date: String): Flow<List<DailyAttachment>> =
        attachmentDao.observeByDate(date)

    suspend fun addAttachment(attachment: DailyAttachment): Long =
        attachmentDao.insert(attachment)

    /**
     * Deletes the backing file from internal storage, then removes the attachment record. Safe to
     * call if the file was already deleted externally. File-then-row order (rather than the
     * reverse) means a crash between the two steps leaves, at worst, a DB row pointing at a
     * missing file - already handled gracefully by [com.mystudytracker.app.ui.checklist.openAttachment]'s
     * existence check - instead of a silently orphaned file with no DB reference to ever clean it up.
     */
    suspend fun removeAttachment(id: Long) {
        val path = attachmentDao.getFilePath(id)
        path?.let { File(it).delete() }
        attachmentDao.deleteById(id)
    }
}

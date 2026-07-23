package com.mystudytracker.app.data

import androidx.room.withTransaction
import com.mystudytracker.app.data.backup.BackupPayload
import com.mystudytracker.app.data.backup.toBackupEntry
import com.mystudytracker.app.data.backup.toDailyAttachment
import com.mystudytracker.app.data.backup.toDailyProgress
import com.mystudytracker.app.data.backup.toDailyTaskState
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * Thin data-access layer between the ViewModels and Room - keeps DAO/SQL details out of the UI
 * layer. Owns [DailyProgressDao] (day-level lock/note/aggregate), [DailyTaskStateDao] (per-task
 * state) and [DailyAttachmentDao] (per-day file attachments).
 */
class ProgressRepository(
    private val db: AppDatabase,
    private val dao: DailyProgressDao,
    private val taskStateDao: DailyTaskStateDao,
    private val attachmentDao: DailyAttachmentDao
) {

    fun observeAll(): Flow<List<DailyProgress>> = dao.observeAll()

    fun observeByDate(date: String): Flow<DailyProgress?> = dao.observeByDate(date)

    fun observeTaskStates(date: String): Flow<List<DailyTaskState>> = taskStateDao.observeByDate(date)

    /** Permanently finalizes a day. There is no corresponding "unlock" - this cannot be undone. */
    suspend fun lockDay(date: String) {
        dao.setLocked(date)
    }

    /**
     * Saves the free-form remark for [date], independent of task state and lock status. Creates a
     * fresh row (no tasks touched yet) if [date] has no saved progress yet.
     */
    suspend fun saveNote(date: String, note: String?) {
        val rowsUpdated = dao.updateNote(date, note)
        if (rowsUpdated == 0) {
            dao.upsert(DailyProgress(date = date, note = note))
        }
    }

    // ── Task state (checklist) ────────────────────────────────────────────────────

    /**
     * Toggles a single leaf's completion. If [targetCount] is 1 this is a plain done/undone flip;
     * for a leaf with a quantity > 1 it cycles 0 -> 1 -> ... -> target -> 0, so a single tap still
     * always makes forward progress. No-op while [notApplicable] is already set - use
     * [setLeafNotApplicable] to clear that first.
     */
    suspend fun toggleLeaf(date: String, taskKey: String, current: DailyTaskState?) {
        if (current?.notApplicable == true) return
        val target = current?.targetCount ?: 1
        val completed = current?.completedCount ?: 0
        val next = if (completed >= target) 0 else completed + 1
        writeLeaf(date, taskKey, completedCount = next, targetCount = target, notApplicable = false)
    }

    /** Sets an explicit quantity for a leaf, clamping its current progress so it never exceeds the new target. */
    suspend fun setLeafQuantity(date: String, taskKey: String, current: DailyTaskState?, newTarget: Int) {
        val target = newTarget.coerceAtLeast(1)
        val completed = (current?.completedCount ?: 0).coerceAtMost(target)
        writeLeaf(date, taskKey, completedCount = completed, targetCount = target, notApplicable = current?.notApplicable ?: false)
    }

    /** Toggles a single leaf's "doesn't apply" flag, leaving its stored quantity/progress untouched underneath. */
    suspend fun setLeafNotApplicable(date: String, taskKey: String, current: DailyTaskState?, notApplicable: Boolean) {
        writeLeaf(
            date, taskKey,
            completedCount = current?.completedCount ?: 0,
            targetCount = current?.targetCount ?: 1,
            notApplicable = notApplicable
        )
    }

    /**
     * Cascades a check/uncheck to every leaf under a section or group. If any descendant isn't
     * yet fully done, this completes all of them (also clearing "doesn't apply" so they count);
     * if every descendant is already fully done, this resets all of them to zero progress.
     */
    suspend fun toggleGroup(date: String, leafKeys: List<String>, currentByKey: Map<String, DailyTaskState?>) {
        val allDone = leafKeys.all { currentByKey[it].isDone() }
        db.withTransaction {
            val rows = leafKeys.map { key ->
                val existing = currentByKey[key]
                val target = existing?.targetCount ?: 1
                DailyTaskState(
                    date = date,
                    taskKey = key,
                    completedCount = if (allDone) 0 else target,
                    targetCount = target,
                    notApplicable = false
                )
            }
            taskStateDao.upsertAll(rows)
            recomputeAggregate(date)
        }
    }

    /** Cascades "doesn't apply" to every leaf under a section or group - toggle off if all are already excluded. */
    suspend fun toggleGroupNotApplicable(date: String, leafKeys: List<String>, currentByKey: Map<String, DailyTaskState?>) {
        val allExcluded = leafKeys.all { currentByKey[it]?.notApplicable == true }
        db.withTransaction {
            val rows = leafKeys.map { key ->
                val existing = currentByKey[key]
                DailyTaskState(
                    date = date,
                    taskKey = key,
                    completedCount = existing?.completedCount ?: 0,
                    targetCount = existing?.targetCount ?: 1,
                    notApplicable = !allExcluded
                )
            }
            taskStateDao.upsertAll(rows)
            recomputeAggregate(date)
        }
    }

    private suspend fun writeLeaf(date: String, taskKey: String, completedCount: Int, targetCount: Int, notApplicable: Boolean) {
        db.withTransaction {
            taskStateDao.upsert(DailyTaskState(date, taskKey, completedCount, targetCount, notApplicable))
            recomputeAggregate(date)
        }
    }

    /** Recomputes the date's denormalized completed/total units from scratch across every catalog leaf. */
    private suspend fun recomputeAggregate(date: String) {
        val rowsByKey = taskStateDao.getByDate(date).associateBy { it.taskKey }
        var completed = 0
        var total = 0
        for (key in TaskCatalog.allLeafKeys) {
            val row = rowsByKey[key]
            total += row.effectiveTarget()
            completed += row.effectiveCompleted()
        }
        dao.upsertAggregate(date, completed, total)
    }

    // ── Backlog report ────────────────────────────────────────────────────────────

    /**
     * Pending unit count for every catalog leaf, up to and including [throughDate]. A leaf with no
     * saved rows at all for the whole range is still reported (1 pending per untouched tracked
     * day), matching the calendar's "no row = fully outstanding" convention.
     */
    suspend fun backlogByLeaf(throughDate: String, trackedDayCount: Int): Map<String, Int> {
        val rows = taskStateDao.backlogThrough(throughDate).associateBy { it.taskKey }
        return TaskCatalog.allLeafKeys.associateWith { key ->
            val row = rows[key]
            val fromRows = row?.pendingFromRows ?: 0
            val touchedDays = row?.touchedDays ?: 0
            val untouchedDays = (trackedDayCount - touchedDays).coerceAtLeast(0)
            fromRows + untouchedDays
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

    // ── Backup & Restore ──────────────────────────────────────────────────────────────────────

    /** Collects every row from all three tables into a single [BackupPayload] for encryption. */
    suspend fun getAllDataForBackup(): BackupPayload = BackupPayload(
        exportedAt = System.currentTimeMillis(),
        dailyProgress    = dao.getAll().map          { it.toBackupEntry() },
        dailyTaskStates  = taskStateDao.getAll().map  { it.toBackupEntry() },
        dailyAttachments = attachmentDao.getAll().map { it.toBackupEntry() }
    )

    /**
     * Atomically replaces all data with the contents of [payload]. The transaction ensures
     * that a crash mid-restore never leaves the database in a partial state — either the
     * old data is intact or the new data is fully in place.
     *
     * Attachment rows are re-inserted with id = 0 so Room auto-assigns new primary keys;
     * the file paths they reference remain valid on the same device.
     */
    suspend fun restoreFromBackup(payload: BackupPayload) {
        db.withTransaction {
            dao.deleteAll()
            taskStateDao.deleteAll()
            attachmentDao.deleteAll()

            // mapNotNull + null-guard on required fields defends against any entry whose
            // required fields are null (e.g. a backup written by an older obfuscated build
            // that somehow still passed the top-level payload null-check). We skip corrupt
            // rows rather than letting a NullPointerException abort the whole restore.
            dao.upsertAll(
                payload.dailyProgress.mapNotNull { entry ->
                    if (entry?.date == null) null else entry.toDailyProgress()
                }
            )
            taskStateDao.upsertAll(
                payload.dailyTaskStates.mapNotNull { entry ->
                    if (entry?.date == null || entry.taskKey == null) null else entry.toDailyTaskState()
                }
            )
            attachmentDao.insertAll(
                payload.dailyAttachments.mapNotNull { entry ->
                    if (entry?.date == null || entry.filePath == null || entry.type == null) null
                    else runCatching { entry.toDailyAttachment() }.getOrNull()
                }
            )
        }
    }
}

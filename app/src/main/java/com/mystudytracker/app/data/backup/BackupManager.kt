package com.mystudytracker.app.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mystudytracker.app.data.AttachmentType
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// AES-256 key compiled into the app. Never leaves the device.
// Obfuscated further by R8/ProGuard in release builds.
@Suppress("SpellCheckingInspection")
private val BACKUP_KEY = byteArrayOf(
    0x4D, 0x53, 0x54, 0x42, 0x61, 0x63, 0x6B, 0x75,
    0x70, 0x4B, 0x65, 0x79, 0x32, 0x30, 0x32, 0x36,
    0x21, 0x40, 0x23, 0x24, 0x25, 0x5E, 0x26, 0x2A,
    0x28, 0x29, 0x5F, 0x2B, 0x3D, 0x7C, 0x7B, 0x7D
)

// Magic header written as the first 4 bytes so we can reject non-MSTB files before
// attempting decryption (avoids a confusing "corrupted" error for entirely wrong files).
private val MAGIC = byteArrayOf(0x4D, 0x53, 0x54, 0x42) // "MSTB"

private const val GCM_IV_LENGTH  = 12
private const val GCM_TAG_BITS   = 128
private const val CIPHER_ALGO    = "AES/GCM/NoPadding"
private const val KEY_ALGO       = "AES"

sealed class BackupResult {
    object Success : BackupResult()
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val payload: BackupPayload) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

object BackupManager {

    private val gson   = Gson()
    private val random = SecureRandom()

    // ── Encrypt ───────────────────────────────────────────────────────────────

    private fun encrypt(payload: BackupPayload): ByteArray {
        val plaintext = gson.toJson(payload).toByteArray(Charsets.UTF_8)
        val iv = ByteArray(GCM_IV_LENGTH).also { random.nextBytes(it) }

        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(BACKUP_KEY, KEY_ALGO),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        val ciphertext = cipher.doFinal(plaintext)

        // Layout: [4-byte magic][12-byte IV][ciphertext + 16-byte GCM tag]
        return MAGIC + iv + ciphertext
    }

    // ── Decrypt ───────────────────────────────────────────────────────────────

    private fun decrypt(bytes: ByteArray): RestoreResult {
        if (bytes.size < MAGIC.size + GCM_IV_LENGTH + 1) {
            return RestoreResult.Error("This file is not a valid My Study Tracker backup.")
        }

        // Verify magic header
        val header = bytes.copyOfRange(0, MAGIC.size)
        if (!header.contentEquals(MAGIC)) {
            return RestoreResult.Error("This file is not a valid My Study Tracker backup.")
        }

        val iv         = bytes.copyOfRange(MAGIC.size, MAGIC.size + GCM_IV_LENGTH)
        val ciphertext = bytes.copyOfRange(MAGIC.size + GCM_IV_LENGTH, bytes.size)

        val plaintext = try {
            val cipher = Cipher.getInstance(CIPHER_ALGO)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(BACKUP_KEY, KEY_ALGO),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            // GCM authentication failure means the file was tampered with or is corrupt.
            return RestoreResult.Error("Backup file is corrupted or has been modified and cannot be restored.")
        }

        return try {
            val payload = parsePayload(String(plaintext, Charsets.UTF_8))
            if (payload == null) {
                RestoreResult.Error("Backup file could not be read. It may be from an incompatible version.")
            } else {
                RestoreResult.Success(payload)
            }
        } catch (_: Exception) {
            RestoreResult.Error("Backup file could not be read. It may be from an incompatible version.")
        }
    }

    // ── Payload parsing (primary + legacy fallback) ───────────────────────────

    /**
     * Two-stage parse:
     *
     * 1. Primary — Gson + @SerializedName: works for every build that has the
     *    annotations (all builds from this commit onwards).
     *
     * 2. Legacy fallback — raw JsonObject walk: the original release build had no
     *    @SerializedName and no ProGuard keep rules, so R8 renamed each field to a
     *    single letter in declaration order (a, b, c, d, e per class). We try both
     *    the real name and the likely obfuscated name for every field so those old
     *    backups continue to load transparently.
     */
    private fun parsePayload(json: String): BackupPayload? {
        // Stage 1: standard parse with @SerializedName annotations
        val primary = runCatching { gson.fromJson(json, BackupPayload::class.java) }.getOrNull()
        if (primary?.dailyProgress != null
            && primary.dailyTaskStates != null
            && primary.dailyAttachments != null) {
            return primary
        }

        // Stage 2: legacy raw-object parse for backups written by the first
        // release build, where R8 had renamed every field to a single letter.
        return runCatching { parseLegacy(json) }.getOrNull()
    }

    // ── Legacy parser helpers ─────────────────────────────────────────────────

    /** Returns the first non-null String value found under any of [keys]. */
    private fun JsonObject.str(vararg keys: String): String? {
        for (k in keys) {
            val el = get(k) ?: continue
            if (!el.isJsonNull) return runCatching { el.asString }.getOrNull() ?: continue
        }
        return null
    }

    /** Returns the first non-null Int found under any of [keys], or [default]. */
    private fun JsonObject.int_(vararg keys: String, default: Int = 0): Int {
        for (k in keys) {
            val el = get(k) ?: continue
            if (!el.isJsonNull) return runCatching { el.asInt }.getOrNull() ?: continue
        }
        return default
    }

    /** Returns the first non-null Long found under any of [keys], or [default]. */
    private fun JsonObject.long_(vararg keys: String, default: Long = 0L): Long {
        for (k in keys) {
            val el = get(k) ?: continue
            if (!el.isJsonNull) return runCatching { el.asLong }.getOrNull() ?: continue
        }
        return default
    }

    /** Returns the first non-null Boolean found under any of [keys], or [default]. */
    private fun JsonObject.bool_(vararg keys: String, default: Boolean = false): Boolean {
        for (k in keys) {
            val el = get(k) ?: continue
            if (!el.isJsonNull) return runCatching { el.asBoolean }.getOrNull() ?: continue
        }
        return default
    }

    /**
     * Parse a BackupPayload written by the original obfuscated release build.
     *
     * Field name mapping (real name → likely R8 single-letter name):
     *
     *   BackupPayload:         version→a  exportedAt→b  dailyProgress→c  dailyTaskStates→d  dailyAttachments→e
     *   DailyProgressEntry:    date→a  completedUnits→b  totalUnits→c  locked→d  note→e
     *   DailyTaskStateEntry:   date→a  taskKey→b  completedCount→c  targetCount→d  notApplicable→e
     *   DailyAttachmentEntry:  date→a  filePath→b  type→c  displayName→d  addedAt→e
     */
    private fun parseLegacy(json: String): BackupPayload? {
        val root = JsonParser.parseString(json).asJsonObject

        // Top-level arrays — try real name first, then likely obfuscated name
        val progressArr  = root.getAsJsonArray("dailyProgress")    ?: root.getAsJsonArray("c") ?: return null
        val taskArr      = root.getAsJsonArray("dailyTaskStates")   ?: root.getAsJsonArray("d") ?: return null
        val attachArr    = root.getAsJsonArray("dailyAttachments")  ?: root.getAsJsonArray("e") ?: return null

        val version     = root.int_("version",    "a", default = 1)
        val exportedAt  = root.long_("exportedAt", "b", default = 0L)

        val progress = progressArr.mapNotNull { el ->
            runCatching {
                val o = el.asJsonObject
                val date = o.str("date", "a") ?: return@mapNotNull null
                DailyProgressEntry(
                    date           = date,
                    completedUnits = o.int_("completedUnits", "b"),
                    totalUnits     = o.int_("totalUnits",     "c"),
                    locked         = o.bool_("locked",        "d"),
                    note           = o.str("note",            "e")   // nullable — str() already handles JsonNull
                )
            }.getOrNull()
        }

        val tasks = taskArr.mapNotNull { el ->
            runCatching {
                val o = el.asJsonObject
                val date    = o.str("date",    "a") ?: return@mapNotNull null
                val taskKey = o.str("taskKey", "b") ?: return@mapNotNull null
                DailyTaskStateEntry(
                    date           = date,
                    taskKey        = taskKey,
                    completedCount = o.int_("completedCount", "c"),
                    targetCount    = o.int_("targetCount",    "d", default = 1),
                    notApplicable  = o.bool_("notApplicable", "e")
                )
            }.getOrNull()
        }

        val attachments = attachArr.mapNotNull { el ->
            runCatching {
                val o = el.asJsonObject
                val date        = o.str("date",        "a") ?: return@mapNotNull null
                val filePath    = o.str("filePath",    "b") ?: return@mapNotNull null
                val type        = o.str("type",        "c") ?: return@mapNotNull null
                val displayName = o.str("displayName", "d") ?: ""
                val addedAt     = o.long_("addedAt",   "e")
                DailyAttachmentEntry(date, filePath, type, displayName, addedAt)
            }.getOrNull()
        }

        return BackupPayload(version, exportedAt, progress, tasks, attachments)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun writeToUri(context: Context, uri: Uri, payload: BackupPayload): BackupResult {
        return try {
            // Embed each attachment's raw bytes as base64 so the file can be fully
            // reconstructed on restore, even on a different device or after the original
            // file has been deleted. Attachments whose backing file is already missing are
            // included without content (fileContent = null) so the record is at least
            // preserved in the database with its display name and date.
            val enriched = payload.copy(
                dailyAttachments = payload.dailyAttachments.map { entry ->
                    val file = File(entry.filePath)
                    val content = if (file.exists()) {
                        runCatching {
                            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                        }.getOrNull()
                    } else null
                    if (content != null) entry.copy(fileContent = content) else entry
                }
            )
            val bytes = encrypt(enriched)
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return BackupResult.Error("Could not open the selected file for writing.")
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Error("Backup failed: ${e.message}")
        }
    }

    fun readFromUri(context: Context, uri: Uri): RestoreResult {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return RestoreResult.Error("Could not open the selected file.")
            val result = decrypt(bytes)
            // After decryption/parsing, write any embedded file bytes to internal storage
            // and rewrite each entry's filePath to the new local path so the DB record
            // points to a real, openable file on this device.
            if (result is RestoreResult.Success) {
                RestoreResult.Success(extractAttachmentFiles(context, result.payload))
            } else {
                result
            }
        } catch (e: Exception) {
            RestoreResult.Error("Could not read file: ${e.message}")
        }
    }

    /**
     * For each [DailyAttachmentEntry] that carries embedded [fileContent], writes the raw
     * bytes to app-internal storage under `attachments/<date>/<type>/` (matching the path
     * structure used by [copyToInternalStorage] in ChecklistScreen) and returns a copy of
     * the payload with each entry's [filePath] updated to the new absolute path and
     * [fileContent] cleared so the bytes aren't written to the database.
     *
     * Entries with no [fileContent] (legacy backups) are passed through unchanged; they will
     * fail gracefully at open time with a user-visible message if the file is truly gone.
     */
    private fun extractAttachmentFiles(context: Context, payload: BackupPayload): BackupPayload {
        val restored = payload.dailyAttachments.map { entry ->
            val content = entry.fileContent ?: return@map entry
            val type = runCatching { AttachmentType.valueOf(entry.type) }.getOrNull()
                ?: return@map entry
            val ext = entry.displayName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
            val fileName = "${UUID.randomUUID()}${if (ext != null) ".$ext" else ""}"
            val dir = File(context.filesDir, "attachments/${entry.date}/${type.name.lowercase()}")
            dir.mkdirs()
            val dest = File(dir, fileName)
            runCatching {
                dest.writeBytes(Base64.decode(content, Base64.NO_WRAP))
                // Clear fileContent once written — it must not reach the database
                entry.copy(filePath = dest.absolutePath, fileContent = null)
            }.getOrElse { entry } // if write fails, keep original entry; open will show a clear error
        }
        return payload.copy(dailyAttachments = restored)
    }
}

package com.mystudytracker.app.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import java.security.SecureRandom
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
            val payload = gson.fromJson(String(plaintext, Charsets.UTF_8), BackupPayload::class.java)
            RestoreResult.Success(payload)
        } catch (_: Exception) {
            RestoreResult.Error("Backup file could not be read. It may be from an incompatible version.")
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun writeToUri(context: Context, uri: Uri, payload: BackupPayload): BackupResult {
        return try {
            val bytes = encrypt(payload)
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
            decrypt(bytes)
        } catch (e: Exception) {
            RestoreResult.Error("Could not read file: ${e.message}")
        }
    }
}

package com.mystudytracker.app.util

import android.content.Context
import android.os.SystemClock
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Computes "today" from the device's uptime clock (SystemClock.elapsedRealtime()), anchored to a
 * calendar date that was last verified over the network. This makes the tracked date immune to the
 * user manually changing the phone's wall-clock date/time.
 *
 * The network is NEVER touched automatically - only [syncNow] (triggered by an explicit user tap)
 * makes a request. Everything else here is a couple of SharedPreferences reads plus a single
 * SystemClock call, so the cost of using this on every app open is negligible.
 */
class DateIntegrityManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class State(
        val today: LocalDate?,
        val lastSyncedLabel: String?,
        val rebootDetected: Boolean,
        val needsFirstSync: Boolean
    )

    sealed class SyncResult {
        data class Success(val date: LocalDate, val label: String) : SyncResult()
        object Failure : SyncResult()
    }

    /**
     * Call on every app open (cheap, no network). Recomputes "today" from anchor + elapsed uptime,
     * or reports that a reboot happened / no anchor exists yet.
     */
    fun currentState(): State {
        val anchorEpochDay = prefs.getLong(KEY_ANCHOR_EPOCH_DAY, NO_VALUE)
        val anchorElapsed = prefs.getLong(KEY_ANCHOR_ELAPSED, NO_VALUE)
        val lastElapsed = prefs.getLong(KEY_LAST_ELAPSED, NO_VALUE)
        val lastSyncedLabel = prefs.getString(KEY_LAST_SYNCED_LABEL, null)

        if (anchorEpochDay == NO_VALUE || anchorElapsed == NO_VALUE) {
            return State(today = null, lastSyncedLabel = null, rebootDetected = false, needsFirstSync = true)
        }

        val nowElapsed = SystemClock.elapsedRealtime()

        // elapsedRealtime() resets to (near) zero on every reboot, so if it's now smaller than the
        // last value we recorded, the device restarted since the last open - we cannot trust any
        // uptime-based math until the user re-syncs, so we freeze on the last confirmed date.
        if (lastElapsed != NO_VALUE && nowElapsed < lastElapsed) {
            val frozenDate = LocalDate.ofEpochDay(anchorEpochDay)
            return State(today = frozenDate, lastSyncedLabel = lastSyncedLabel, rebootDetected = true, needsFirstSync = false)
        }

        prefs.edit().putLong(KEY_LAST_ELAPSED, nowElapsed).apply()

        val daysSinceAnchor = (nowElapsed - anchorElapsed) / MS_PER_DAY
        val today = LocalDate.ofEpochDay(anchorEpochDay + daysSinceAnchor)
        return State(today = today, lastSyncedLabel = lastSyncedLabel, rebootDetected = false, needsFirstSync = false)
    }

    /**
     * Performs exactly one lightweight HTTPS request (a HEAD request against a large, always-up
     * host) and reads the standard "Date" response header to get a verified calendar date, then
     * re-anchors the uptime tracker to it. Only ever called from an explicit user tap - never
     * automatically.
     */
    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(TIME_SOURCE_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            val dateHeader: String?
            try {
                connection.connect()
                dateHeader = connection.getHeaderField("Date")
            } finally {
                connection.disconnect()
            }

            if (dateHeader == null) return@withContext SyncResult.Failure

            val serverInstant = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(dateHeader))
            val verifiedDate = serverInstant.atZone(ZoneId.systemDefault()).toLocalDate()
            val nowElapsed = SystemClock.elapsedRealtime()
            val label = LABEL_FORMAT.format(serverInstant.atZone(ZoneId.systemDefault()))

            prefs.edit()
                .putLong(KEY_ANCHOR_EPOCH_DAY, verifiedDate.toEpochDay())
                .putLong(KEY_ANCHOR_ELAPSED, nowElapsed)
                .putLong(KEY_LAST_ELAPSED, nowElapsed)
                .putString(KEY_LAST_SYNCED_LABEL, label)
                .apply()

            SyncResult.Success(verifiedDate, label)
        } catch (e: Exception) {
            SyncResult.Failure
        }
    }

    companion object {
        private const val PREFS_NAME = "date_integrity_prefs"
        private const val KEY_ANCHOR_EPOCH_DAY = "anchor_epoch_day"
        private const val KEY_ANCHOR_ELAPSED = "anchor_elapsed_realtime"
        private const val KEY_LAST_ELAPSED = "last_elapsed_realtime"
        private const val KEY_LAST_SYNCED_LABEL = "last_synced_label"
        private const val NO_VALUE = -1L
        private const val MS_PER_DAY = 86_400_000L
        private const val TIMEOUT_MS = 6000
        // A large, always-up HTTPS host, used only to read the standard "Date" response header.
        // Chosen over raw NTP (UDP/123, often blocked on mobile/corporate networks) and over a
        // niche time API (single point of failure) for reliability.
        private const val TIME_SOURCE_URL = "https://www.google.com"
        private val LABEL_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
    }
}

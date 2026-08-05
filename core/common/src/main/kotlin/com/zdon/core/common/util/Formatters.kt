package com.zdon.core.common.util

import java.util.Locale
import java.util.concurrent.TimeUnit

/** Formatting helpers shared by the UI and the notification layer. */
object Formatters {

    private const val BYTES_IN_UNIT = 1024.0
    private val SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

    /** `1.4 GB`, or `null` when the size is unknown. */
    fun formatBytesOrNull(bytes: Long): String? =
        if (bytes <= 0L) null else formatBytes(bytes)

    /** `1.4 GB`. Returns `0 B` for non-positive input. */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 ${SIZE_UNITS.first()}"
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= BYTES_IN_UNIT && unitIndex < SIZE_UNITS.lastIndex) {
            value /= BYTES_IN_UNIT
            unitIndex++
        }
        val pattern = if (unitIndex == 0) "%.0f %s" else "%.1f %s"
        return String.format(Locale.US, pattern, value, SIZE_UNITS[unitIndex])
    }

    /** `2.4 MB/s`. Returns `null` when no speed has been reported yet. */
    fun formatSpeedOrNull(bytesPerSecond: Long): String? =
        if (bytesPerSecond <= 0L) null else "${formatBytes(bytesPerSecond)}/s"

    /** `01:12:33` or `12:33`. Returns `null` for unknown durations. */
    fun formatDurationOrNull(seconds: Long): String? =
        if (seconds <= 0L) null else formatDuration(seconds)

    /** `01:12:33` or `12:33`. Clamps negatives to zero. */
    fun formatDuration(seconds: Long): String {
        val safeSeconds = seconds.coerceAtLeast(0L)
        val hours = TimeUnit.SECONDS.toHours(safeSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(safeSeconds) % MINUTES_PER_HOUR
        val secs = safeSeconds % SECONDS_PER_MINUTE
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
    }

    /** `1.2M`, `13.4K`, `987`. Returns `null` when the count is unknown. */
    fun formatCountOrNull(count: Long?): String? {
        if (count == null || count < 0L) return null
        return when {
            count >= BILLION -> String.format(Locale.US, "%.1fB", count / BILLION.toDouble())
            count >= MILLION -> String.format(Locale.US, "%.1fM", count / MILLION.toDouble())
            count >= THOUSAND -> String.format(Locale.US, "%.1fK", count / THOUSAND.toDouble())
            else -> count.toString()
        }
    }

    /** `42%`. */
    fun formatPercent(percent: Float): String =
        String.format(Locale.US, "%.0f%%", percent.coerceIn(0f, PERCENT_MAX))

    private const val MINUTES_PER_HOUR = 60L
    private const val SECONDS_PER_MINUTE = 60L
    private const val THOUSAND = 1_000L
    private const val MILLION = 1_000_000L
    private const val BILLION = 1_000_000_000L
    private const val PERCENT_MAX = 100f
}

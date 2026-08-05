package com.zdon.core.engine

import com.zdon.core.model.DownloadProgress

/**
 * Parses yt-dlp's progress output.
 *
 * The engine passes `--newline` and a machine-readable
 * `--progress-template`, so the fast path is a single delimited line. The
 * human-readable `[download]  42.0% of 12.34MiB at 1.23MiB/s ETA 00:15` form is
 * still handled because post-processors and external downloaders bypass the
 * template.
 */
internal object ProgressParser {

    /** Field separator used in our `--progress-template`. */
    const val FIELD_SEPARATOR: String = "\u001F"

    /** Marker prefix identifying our own template lines. */
    const val TEMPLATE_PREFIX: String = "ZDON_PROGRESS"

    /**
     * yt-dlp progress template. Values are emitted in a fixed order and joined by
     * [FIELD_SEPARATOR], which removes all ambiguity from locale-formatted text.
     */
    val PROGRESS_TEMPLATE: String = listOf(
        TEMPLATE_PREFIX,
        "%(progress._percent_str)s",
        "%(progress.downloaded_bytes)s",
        "%(progress.total_bytes)s",
        "%(progress.total_bytes_estimate)s",
        "%(progress.speed)s",
        "%(progress.eta)s",
        "%(progress.fragment_index)s",
        "%(progress.fragment_count)s",
    ).joinToString(FIELD_SEPARATOR)

    private val HUMAN_PROGRESS = Regex(
        """\[download]\s+(\d{1,3}(?:\.\d+)?)%\s+of\s+~?\s*([\d.]+)([KMGT]?i?B)""" +
            """(?:\s+at\s+([\d.]+)([KMGT]?i?B)/s)?(?:\s+ETA\s+([\d:]+))?""",
        RegexOption.IGNORE_CASE,
    )

    private val FRAGMENT = Regex("""\(frag\s+(\d+)(?:/(\d+))?\)""", RegexOption.IGNORE_CASE)
    private val DESTINATION = Regex("""\[download]\s+Destination:\s+(.+)""")
    private val ALREADY_DOWNLOADED = Regex("""\[download]\s+(.+)\s+has already been downloaded""")
    private val MERGING = Regex("""\[Merger]\s+Merging formats into\s+"(.+)"""")

    /**
     * Parses [line] and returns an updated snapshot, or `null` when the line
     * carries no progress information.
     */
    fun parse(line: String, previous: DownloadProgress): DownloadProgress? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        parseTemplate(trimmed, previous)?.let { return it }
        parseHuman(trimmed, previous)?.let { return it }
        return null
    }

    /** Extracts the output file path from destination/merge/already-done lines. */
    fun parseDestination(line: String): String? {
        DESTINATION.find(line)?.let { return it.groupValues[1].trim() }
        MERGING.find(line)?.let { return it.groupValues[1].trim() }
        ALREADY_DOWNLOADED.find(line)?.let { return it.groupValues[1].trim() }
        return null
    }

    private fun parseTemplate(line: String, previous: DownloadProgress): DownloadProgress? {
        if (!line.startsWith(TEMPLATE_PREFIX)) return null
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size < TEMPLATE_FIELD_COUNT) return null

        val percent = parts[INDEX_PERCENT].trim().removeSuffix("%").toFloatOrNull()
            ?: previous.percent
        val downloaded = parts[INDEX_DOWNLOADED].toLongOrNullSafe() ?: previous.downloadedBytes
        val total = parts[INDEX_TOTAL].toLongOrNullSafe()
            ?: parts[INDEX_TOTAL_ESTIMATE].toLongOrNullSafe()
            ?: previous.totalBytes
        val speed = parts[INDEX_SPEED].toDoubleOrNullSafe()?.toLong() ?: 0L
        val eta = parts[INDEX_ETA].toLongOrNullSafe() ?: -1L
        val fragmentIndex = parts[INDEX_FRAGMENT_INDEX].toLongOrNullSafe()
        val fragmentCount = parts[INDEX_FRAGMENT_COUNT].toLongOrNullSafe()

        return previous.copy(
            percent = percent.coerceIn(0f, MAX_PERCENT),
            downloadedBytes = downloaded,
            totalBytes = total,
            speedBytesPerSecond = speed.coerceAtLeast(0L),
            etaSeconds = eta,
            fragment = formatFragment(fragmentIndex, fragmentCount) ?: previous.fragment,
            rawLine = line,
        )
    }

    private fun parseHuman(line: String, previous: DownloadProgress): DownloadProgress? {
        val match = HUMAN_PROGRESS.find(line) ?: return null
        val percent = match.groupValues[1].toFloatOrNull() ?: previous.percent
        val totalValue = match.groupValues[2].toDoubleOrNull()
        val totalUnit = match.groupValues[3]
        val speedValue = match.groupValues[4].toDoubleOrNull()
        val speedUnit = match.groupValues[5]
        val etaText = match.groupValues[6]

        val total = totalValue?.let { toBytes(it, totalUnit) } ?: previous.totalBytes
        val downloaded = if (total > 0L) {
            (total * (percent / MAX_PERCENT)).toLong()
        } else {
            previous.downloadedBytes
        }
        val speed = speedValue?.let { toBytes(it, speedUnit) } ?: 0L
        val eta = parseClock(etaText) ?: previous.etaSeconds

        val fragmentMatch = FRAGMENT.find(line)
        val fragment = fragmentMatch?.let {
            formatFragment(
                it.groupValues[1].toLongOrNull(),
                it.groupValues[2].toLongOrNull(),
            )
        } ?: previous.fragment

        return previous.copy(
            percent = percent.coerceIn(0f, MAX_PERCENT),
            downloadedBytes = downloaded,
            totalBytes = total,
            speedBytesPerSecond = speed,
            etaSeconds = eta,
            fragment = fragment,
            rawLine = line,
        )
    }

    private fun formatFragment(index: Long?, count: Long?): String? = when {
        index == null -> null
        count != null && count > 0L -> "$index/$count"
        else -> index.toString()
    }

    private fun parseClock(text: String): Long? {
        if (text.isBlank()) return null
        val segments = text.split(':').mapNotNull { it.toLongOrNull() }
        if (segments.isEmpty() || segments.size != text.split(':').size) return null
        return segments.fold(0L) { accumulator, value -> accumulator * SECONDS_PER_UNIT + value }
    }

    private fun toBytes(value: Double, unit: String): Long {
        val multiplier = when (unit.uppercase().replace("I", "")) {
            "B" -> 1.0
            "KB" -> KIB
            "MB" -> KIB * KIB
            "GB" -> KIB * KIB * KIB
            "TB" -> KIB * KIB * KIB * KIB
            else -> 1.0
        }
        return (value * multiplier).toLong()
    }

    /** yt-dlp prints `NA` for absent numeric values. */
    private fun String.toLongOrNullSafe(): Long? =
        trim().takeIf { it.isNotEmpty() && !it.equals("NA", ignoreCase = true) }
            ?.toDoubleOrNull()
            ?.toLong()

    private fun String.toDoubleOrNullSafe(): Double? =
        trim().takeIf { it.isNotEmpty() && !it.equals("NA", ignoreCase = true) }?.toDoubleOrNull()

    private const val TEMPLATE_FIELD_COUNT = 9
    private const val INDEX_PERCENT = 1
    private const val INDEX_DOWNLOADED = 2
    private const val INDEX_TOTAL = 3
    private const val INDEX_TOTAL_ESTIMATE = 4
    private const val INDEX_SPEED = 5
    private const val INDEX_ETA = 6
    private const val INDEX_FRAGMENT_INDEX = 7
    private const val INDEX_FRAGMENT_COUNT = 8
    private const val MAX_PERCENT = 100f
    private const val SECONDS_PER_UNIT = 60L
    private const val KIB = 1024.0
}

package com.zdon.core.engine

/**
 * A parsed yt-dlp release version such as `2025.11.12`.
 *
 * yt-dlp uses date-based versions in the form `YYYY.MM.DD`. The three
 * components are compared numerically, so `2025.11.12 > 2025.4.5` holds even
 * though a string comparison would order them incorrectly.
 */
data class YtDlpVersion(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<YtDlpVersion> {

    override fun compareTo(other: YtDlpVersion): Int =
        year.compareTo(other.year)
            .takeIf { it != 0 }
            ?: month.compareTo(other.month)
            .takeIf { it != 0 }
            ?: day.compareTo(other.day)

    override fun toString(): String = "$year.$month.$day"

    companion object {
        private val FORMAT = Regex("""(\d{4})\.(\d{1,2})\.(\d{1,2})""")

        /**
         * Parses the output of `yt-dlp --version` (for example `2025.11.12`).
         * Nightly/master builds append a build suffix which is ignored; only the
         * leading date triple is used. Returns `null` for anything unrecognisable.
         */
        fun parse(raw: String?): YtDlpVersion? {
            val match = FORMAT.find(raw?.trim().orEmpty()) ?: return null
            val year = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val day = match.groupValues[3].toIntOrNull() ?: return null
            return YtDlpVersion(year, month, day)
        }
    }
}

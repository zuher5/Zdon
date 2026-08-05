package com.zdon.core.engine

/**
 * Registry of every yt-dlp option Zdon may emit.
 *
 * [DOCUMENTED_OPTIONS] lists the canonical long-form name of each option,
 * kept in sync with the current official yt-dlp documentation. [MINIMUM_VERSIONS]
 * records the first release that introduced an option so commands can be built
 * for the *installed* binary instead of the newest one. Anything not listed
 * here is never passed to yt-dlp, and anything below the detected version's
 * introduction release is silently dropped rather than crashing the run.
 *
 * Only options that exist in the current official yt-dlp documentation are
 * listed; options that were removed (for example `--no-part-file-warning`)
 * are deliberately absent so the availability check rejects them.
 */
object YtDlpOptions {

    /**
     * Long-form option names that exist in the current official yt-dlp
     * documentation ("USAGE AND OPTIONS" section of the README).
     */
    val DOCUMENTED_OPTIONS: Set<String> = setOf(
        "--no-cache-dir",
        "--ignore-config",
        "--no-mtime",
        "--color",
        "--no-colors",
        "--proxy",
        "--cookies",
        "--add-headers",
        "--download-archive",
        "--restrict-filenames",
        "--newline",
        "--progress",
        "--progress-template",
        "--continue",
        "--retries",
        "--fragment-retries",
        "--socket-timeout",
        "--no-abort-on-error",
        "--format",
        "--merge-output-format",
        "--extract-audio",
        "--audio-format",
        "--audio-quality",
        "--embed-metadata",
        "--add-metadata",
        "--write-subs",
        "--write-auto-subs",
        "--sub-langs",
        "--embed-subs",
        "--write-thumbnail",
        "--embed-thumbnail",
        "--yes-playlist",
        "--playlist-items",
        "--no-overwrites",
        "--no-playlist",
        "--output",
        "--paths",
        "--dump-single-json",
        "--no-warnings",
        "--flat-playlist",
    )

    /**
     * Maps short options the app emits onto their canonical long-form name so
     * the availability check can be performed against [DOCUMENTED_OPTIONS].
     */
    private val CANONICAL_NAMES: Map<String, String> = mapOf(
        "-f" to "--format",
        "-o" to "--output",
    )

    /**
     * First yt-dlp release that supports the option. Only options whose
     * introduction is a documented fact are listed; every other documented
     * option predates any yt-dlp build the app can run, so no gate is needed.
     */
    val MINIMUM_VERSIONS: Map<String, YtDlpVersion> = mapOf(
        // --progress and --progress-template were added together in 2021.10.09.
        "--progress" to YtDlpVersion(2021, 10, 9),
        "--progress-template" to YtDlpVersion(2021, 10, 9),
        // --color (with the no_color policy) replaced the deprecated
        // --no-colors alias in the 2023.07.06 release.
        "--color" to YtDlpVersion(2023, 7, 6),
    )

    /** Resolves an option name to its canonical long-form name. */
    fun canonicalName(option: String): String = CANONICAL_NAMES[option] ?: option

    /** True when [option] is a documented, current yt-dlp option. */
    fun isDocumented(option: String): Boolean = canonicalName(option) in DOCUMENTED_OPTIONS
}

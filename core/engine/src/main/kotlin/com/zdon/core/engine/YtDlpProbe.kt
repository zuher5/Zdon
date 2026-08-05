package com.zdon.core.engine

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import timber.log.Timber

/**
 * Probes the installed yt-dlp binary through the wrapper library.
 *
 * The library always prepends the private Python runtime and the bundled
 * binary, so probing is equivalent to running `yt-dlp --version` and
 * `yt-dlp --help` on the device. Both options terminate before any URL is
 * touched, so the empty placeholder URL in [YoutubeDLRequest] is never fetched.
 *
 * Probes are best effort: any failure yields `null`, which callers treat as
 * "unknown" and continue with every documented option.
 */
class YtDlpProbe(
    private val executor: ProbeExecutor = LibraryExecutor,
) {

    /** Runs `yt-dlp --version` and returns the parsed version, or null. */
    suspend fun version(): YtDlpVersion? {
        val output = executor.execute(PROBE_VERSION) ?: return null
        return YtDlpVersion.parse(output)
    }

    /**
     * Runs `yt-dlp --help` and returns the set of long-form options it accepts,
     * or null when the probe failed.
     */
    suspend fun helpOptions(): Set<String>? {
        val output = executor.execute(PROBE_HELP) ?: return null
        return parseOptions(output)
    }

    /** Extracts `--long-option` names from yt-dlp's help text. */
    fun parseOptions(helpText: String): Set<String> =
        OPTION_TOKEN.findAll(helpText).mapTo(mutableSetOf()) { it.value }

    private companion object {
        const val PROBE_VERSION = "--version"
        const val PROBE_HELP = "--help"
        val OPTION_TOKEN = Regex("""(?<![\w-])--[a-z][a-z0-9-]*""")
    }
}

/**
 * Executes a single yt-dlp probe argument and returns captured stdout, or
 * null when the probe could not be completed.
 */
fun interface ProbeExecutor {
    suspend fun execute(option: String): String?
}

/** Production [ProbeExecutor] that runs the probe through the library. */
private object LibraryExecutor : ProbeExecutor {
    override suspend fun execute(option: String): String? = try {
        val request = YoutubeDLRequest("").addOption(option)
        YoutubeDL.getInstance()
            .execute(request, null, redirectErrorStream = false, callback = null)
            .out
    } catch (exception: Exception) {
        Timber.w(exception, "yt-dlp probe '%s' failed", option)
        null
    }
}

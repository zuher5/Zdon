package com.zdon.core.engine

import com.zdon.core.model.DownloadErrorType

/**
 * Maps raw yt-dlp stderr onto [DownloadErrorType].
 *
 * yt-dlp has no machine-readable error codes, so classification is done on the
 * message text. Ordering matters: the most specific signature must be checked
 * before more generic ones (for example a 403 caused by expired cookies is
 * reported as [DownloadErrorType.COOKIES_EXPIRED] rather than
 * [DownloadErrorType.FORBIDDEN]).
 */
object ErrorClassifier {

    /**
     * Classifies [rawMessage]. [isOnline] lets a generic failure be attributed to
     * connectivity when the device is known to be offline.
     */
    fun classify(rawMessage: String?, isOnline: Boolean = true): DownloadErrorType {
        val message = rawMessage?.lowercase().orEmpty()

        if (message.isBlank()) {
            return if (isOnline) DownloadErrorType.UNKNOWN else DownloadErrorType.NETWORK
        }

        SIGNATURES.forEach { (type, needles) ->
            if (needles.any { message.contains(it) }) return type
        }

        return if (isOnline) DownloadErrorType.UNKNOWN else DownloadErrorType.NETWORK
    }

    /**
     * Extracts the first meaningful `ERROR:` line so the UI can show the site's
     * own explanation instead of a wall of Python traceback.
     */
    fun extractPrimaryMessage(rawMessage: String?): String? {
        if (rawMessage.isNullOrBlank()) return null
        val lines = rawMessage.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val errorLine = lines.firstOrNull { it.startsWith("ERROR:", ignoreCase = true) }
        val chosen = errorLine ?: lines.lastOrNull { !it.startsWith("WARNING:", ignoreCase = true) }
        return chosen
            ?.removePrefix("ERROR:")
            ?.removePrefix("error:")
            ?.trim()
            ?.take(MAX_MESSAGE_LENGTH)
            ?.takeIf { it.isNotEmpty() }
    }

    private const val MAX_MESSAGE_LENGTH = 400

    /**
     * Ordered signature table. `LinkedHashMap` preserves the priority in which
     * candidates are tested.
     */
    private val SIGNATURES: Map<DownloadErrorType, List<String>> = linkedMapOf(
        DownloadErrorType.FFMPEG_MISSING to listOf(
            "ffmpeg not found",
            "ffprobe and ffmpeg not found",
            "you have requested merging of multiple formats but ffmpeg is not installed",
            "postprocessing: ffmpeg not found",
        ),
        DownloadErrorType.BINARY_MISSING to listOf(
            "no such file or directory: 'yt-dlp'",
            "failed to initialize",
            "cannot execute binary",
            "text file busy",
        ),
        DownloadErrorType.DISK_FULL to listOf(
            "no space left on device",
            "enospc",
            "disk quota exceeded",
        ),
        DownloadErrorType.PERMISSION_DENIED to listOf(
            "permission denied",
            "eacces",
            "operation not permitted",
            "read-only file system",
        ),
        DownloadErrorType.CAPTCHA_REQUIRED to listOf(
            "captcha",
            "confirm you're not a bot",
            "confirm you are not a bot",
            "sign in to confirm you're not a bot",
        ),
        DownloadErrorType.COOKIES_EXPIRED to listOf(
            "cookies are no longer valid",
            "the provided cookies are not valid",
            "your cookies have expired",
            "login required",
            "use --cookies",
            "requested content is not available, use --cookies",
            "members only",
            "this video requires payment",
            "join this channel to get access",
            "authentication required",
        ),
        DownloadErrorType.AGE_RESTRICTED to listOf(
            "age-restricted",
            "age restricted",
            "sign in to confirm your age",
            "inappropriate for some users",
            "content warning",
        ),
        DownloadErrorType.GEO_RESTRICTED to listOf(
            "not available in your country",
            "geo restriction",
            "geo-restricted",
            "blocked in your country",
            "the uploader has not made this video available in your country",
        ),
        DownloadErrorType.PRIVATE_MEDIA to listOf(
            "private video",
            "this video is private",
            "this post is private",
            "members-only",
            "join this channel",
            "video unavailable. this video is private",
        ),
        DownloadErrorType.NOT_FOUND to listOf(
            "http error 404",
            "not found",
            "video unavailable",
            "this video has been removed",
            "the page doesn't exist",
            "has been terminated",
        ),
        DownloadErrorType.FORBIDDEN to listOf(
            "http error 403",
            "forbidden",
            "access denied",
        ),
        DownloadErrorType.FORMAT_UNAVAILABLE to listOf(
            "requested format is not available",
            "requested format not available",
            "no video formats found",
            "only images are available",
        ),
        DownloadErrorType.UNSUPPORTED_URL to listOf(
            "unsupported url",
            "is not a valid url",
            "no suitable extractor",
            "unable to extract",
        ),
        DownloadErrorType.NETWORK to listOf(
            "unable to download webpage",
            "temporary failure in name resolution",
            "connection reset",
            "connection refused",
            "network is unreachable",
            "timed out",
            "timeout",
            "ssl",
            "urlopen error",
            "remote end closed connection",
            "getaddrinfo failed",
            "http error 5",
            "http error 429",
        ),
        DownloadErrorType.INTERRUPTED to listOf(
            "interrupted",
            "killed",
            "sigterm",
            "sigkill",
            "process was terminated",
        ),
    )
}

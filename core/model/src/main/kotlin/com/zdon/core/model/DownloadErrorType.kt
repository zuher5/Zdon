package com.zdon.core.model

/**
 * Classified failure reasons. The download engine maps raw yt-dlp stderr onto
 * these values so the UI can show an actionable, localized message instead of
 * a stack trace, and so retry logic can distinguish transient from permanent
 * failures.
 */
enum class DownloadErrorType {
    /** No network, DNS failure, TLS failure or connection reset. */
    NETWORK,

    /** HTTP 403 - usually expired signature or missing cookies. */
    FORBIDDEN,

    /** HTTP 404 - the media no longer exists. */
    NOT_FOUND,

    /** The extractor reported the media as private. */
    PRIVATE_MEDIA,

    /** Sign-in required to confirm age. */
    AGE_RESTRICTED,

    /** Blocked in the current region. */
    GEO_RESTRICTED,

    /** The site presented a CAPTCHA / bot check. */
    CAPTCHA_REQUIRED,

    /** Supplied cookies were rejected or are stale. */
    COOKIES_EXPIRED,

    /** No space left on the target volume. */
    DISK_FULL,

    /** SAF permission revoked or the tree URI is no longer writable. */
    PERMISSION_DENIED,

    /** The bundled yt-dlp payload could not be initialised. */
    BINARY_MISSING,

    /** FFmpeg is required for the selected operation but unavailable. */
    FFMPEG_MISSING,

    /** The requested format id does not exist for this media. */
    FORMAT_UNAVAILABLE,

    /** Playlist/entry was skipped because it is unavailable. */
    UNSUPPORTED_URL,

    /** Process died or the app was killed mid-transfer. */
    INTERRUPTED,

    /** Anything the classifier could not attribute. */
    UNKNOWN,
    ;

    /** Whether retrying without user intervention can plausibly succeed. */
    val isTransient: Boolean
        get() = when (this) {
            NETWORK, FORBIDDEN, INTERRUPTED, UNKNOWN -> true
            else -> false
        }
}

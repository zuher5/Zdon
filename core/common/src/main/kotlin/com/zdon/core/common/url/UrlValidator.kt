package com.zdon.core.common.url

import com.zdon.core.model.SupportedSite
import java.net.URI
import java.net.URISyntaxException

/**
 * Validates and normalises user-supplied URLs before they reach the download
 * engine. yt-dlp arguments are always passed as a separate `ProcessBuilder`
 * argument, never through a shell, but a URL is still rejected when it contains
 * characters that could be misread as an option (a leading `-`) or that cannot
 * appear in a legal URL.
 */
object UrlValidator {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    /**
     * Characters that must never appear in an accepted URL. Whitespace and shell
     * metacharacters are rejected defensively even though no shell is involved,
     * because yt-dlp itself expands some values (for example `--exec`).
     */
    private val FORBIDDEN_CHARACTERS = charArrayOf(
        '\u0000', '\n', '\r', '\t', ' ', '"', '\'', '`', '\\',
        '$', '|', ';', '&', '<', '>', '(', ')', '{', '}', '*',
    )

    /**
     * Normalises [input] and returns a [UrlValidationResult]. Bare hosts such as
     * `youtu.be/abc` are upgraded to `https://` so pasting from a share sheet
     * works.
     */
    fun validate(input: String): UrlValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return UrlValidationResult.Invalid(UrlError.EMPTY)
        if (trimmed.startsWith("-")) return UrlValidationResult.Invalid(UrlError.MALFORMED)
        if (trimmed.any { it in FORBIDDEN_CHARACTERS }) {
            return UrlValidationResult.Invalid(UrlError.ILLEGAL_CHARACTERS)
        }

        val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"

        val uri = try {
            URI(candidate)
        } catch (_: URISyntaxException) {
            return UrlValidationResult.Invalid(UrlError.MALFORMED)
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme == null || scheme !in ALLOWED_SCHEMES) {
            return UrlValidationResult.Invalid(UrlError.UNSUPPORTED_SCHEME)
        }

        val host = uri.host
        if (host.isNullOrBlank() || !host.contains('.')) {
            return UrlValidationResult.Invalid(UrlError.MALFORMED)
        }

        return UrlValidationResult.Valid(
            url = uri.toASCIIString(),
            site = SupportedSite.fromHost(host),
        )
    }

    /** Convenience predicate for enabling UI affordances while typing. */
    fun isProbablyValid(input: String): Boolean = validate(input) is UrlValidationResult.Valid

    /**
     * Heuristic used to decide whether the analyse step should offer playlist
     * options before yt-dlp has been consulted.
     */
    fun looksLikePlaylist(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("list=") ||
            lower.contains("/playlist") ||
            lower.contains("/sets/") ||
            lower.contains("/album/")
    }
}

/** Outcome of [UrlValidator.validate]. */
sealed interface UrlValidationResult {
    data class Valid(val url: String, val site: SupportedSite?) : UrlValidationResult
    data class Invalid(val error: UrlError) : UrlValidationResult
}

/** Machine-readable validation failures; mapped to string resources by the UI. */
enum class UrlError {
    EMPTY,
    MALFORMED,
    UNSUPPORTED_SCHEME,
    ILLEGAL_CHARACTERS,
}

package com.zdon.core.common.util

/**
 * Sanitises values that are passed to yt-dlp as arguments.
 *
 * The engine never invokes a shell: every argument is a distinct element of the
 * `ProcessBuilder` command list, which removes the classic injection vector.
 * These helpers close the remaining gaps:
 *
 * * a value starting with `-` would be parsed by yt-dlp as another option,
 * * NUL and newline characters can truncate or split arguments,
 * * path separators in a user-provided filename could escape the target folder.
 */
object ArgumentSanitizer {

    private const val REPLACEMENT = '_'
    private const val MAX_FILE_NAME_LENGTH = 180

    private val ILLEGAL_FILE_NAME_CHARS = charArrayOf(
        '/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000', '\n', '\r', '\t',
    )

    /**
     * Removes control characters and refuses values that would be interpreted as
     * an option. Returns `null` when nothing usable remains, so callers can skip
     * adding the option entirely.
     */
    fun sanitizeOptionValue(value: String?): String? {
        val cleaned = value?.trim()
            ?.filter { it.code >= MIN_PRINTABLE_CODE || it == ' ' }
            ?.trim()
        if (cleaned.isNullOrEmpty()) return null
        if (cleaned.startsWith("-")) return null
        return cleaned
    }

    /**
     * Makes [name] safe to use as a single path segment: strips directory
     * separators, reserved characters, leading dots and dashes, and truncates to
     * a length every Android filesystem accepts.
     */
    fun sanitizeFileName(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val mapped = buildString(name.length) {
            name.forEach { character ->
                when {
                    character in ILLEGAL_FILE_NAME_CHARS -> append(REPLACEMENT)
                    character.code < MIN_PRINTABLE_CODE -> append(REPLACEMENT)
                    else -> append(character)
                }
            }
        }
        val collapsed = mapped.replace(REPEATED_UNDERSCORES, REPLACEMENT.toString())
            .trim()
            .trimStart('.', '-')
            .trim()
        if (collapsed.isEmpty()) return null
        return collapsed.take(MAX_FILE_NAME_LENGTH)
    }

    /**
     * Validates a raw yt-dlp format expression such as
     * `bestvideo[height<=?1080]+bestaudio`. Only characters that legitimately
     * appear in format selectors are allowed.
     */
    fun sanitizeFormatExpression(expression: String?): String? {
        val trimmed = expression?.trim()
        if (trimmed.isNullOrEmpty()) return null
        if (trimmed.startsWith("-")) return null
        if (!trimmed.all { it.isLetterOrDigit() || it in FORMAT_ALLOWED_SYMBOLS }) return null
        return trimmed
    }

    /**
     * Validates a `Header: value` pair. Header names are restricted to RFC 7230
     * token characters and values may not contain CR/LF, which prevents header
     * injection.
     */
    fun sanitizeHeader(header: String?): String? {
        val trimmed = header?.trim() ?: return null
        val separatorIndex = trimmed.indexOf(':')
        if (separatorIndex <= 0 || separatorIndex == trimmed.lastIndex) return null
        val name = trimmed.substring(0, separatorIndex).trim()
        val value = trimmed.substring(separatorIndex + 1).trim()
        if (name.isEmpty() || value.isEmpty()) return null
        if (!name.all { it.isLetterOrDigit() || it == '-' || it == '_' }) return null
        if (value.any { it == '\n' || it == '\r' || it.code < MIN_PRINTABLE_CODE }) return null
        return "$name: $value"
    }

    /**
     * Validates a playlist item selector such as `1-5,8,10::2`.
     */
    fun sanitizePlaylistItems(items: String?): String? {
        val trimmed = items?.trim()
        if (trimmed.isNullOrEmpty()) return null
        if (!trimmed.all { it.isDigit() || it == ',' || it == '-' || it == ':' }) return null
        if (trimmed.startsWith("-")) return null
        return trimmed
    }

    /**
     * Validates a comma-separated subtitle language list such as `en,es,fr` or
     * `all`.
     */
    fun sanitizeLanguageList(languages: String?): String? {
        val trimmed = languages?.trim()
        if (trimmed.isNullOrEmpty()) return null
        if (!trimmed.all { it.isLetterOrDigit() || it == ',' || it == '-' || it == '.' }) return null
        return trimmed
    }

    private const val MIN_PRINTABLE_CODE = 0x20
    private val REPEATED_UNDERSCORES = Regex("_{2,}")
    private val FORMAT_ALLOWED_SYMBOLS = charArrayOf(
        '+', '/', '[', ']', '<', '>', '=', '?', '*', '-', '_', '.', ':', '^', '~', ',', ' ',
    )
}

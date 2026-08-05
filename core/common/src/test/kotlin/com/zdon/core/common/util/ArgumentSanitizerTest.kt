package com.zdon.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies the sanitiser closes the remaining argument-handling gaps: options that
 * could be re-interpreted by yt-dlp, path traversal in filenames and CRLF header
 * injection.
 */
class ArgumentSanitizerTest {

    @Test
    fun `rejects an option value that starts with a dash`() {
        assertNull(ArgumentSanitizer.sanitizeOptionValue("--exec=rm -rf /"))
    }

    @Test
    fun `strips control characters from an option value`() {
        assertEquals(
            "socks5://127.0.0.1:9050",
            ArgumentSanitizer.sanitizeOptionValue("socks5://127.0.0.1:9050\n"),
        )
    }

    @Test
    fun `returns null for a blank option value`() {
        assertNull(ArgumentSanitizer.sanitizeOptionValue("   "))
        assertNull(ArgumentSanitizer.sanitizeOptionValue(null))
    }

    @Test
    fun `removes path separators and leading dots from a file name`() {
        // Separators become underscores and the leading dots are stripped, so the
        // value can only ever name a file inside the target directory.
        assertEquals(
            "_.._etc_passwd",
            ArgumentSanitizer.sanitizeFileName("../../etc/passwd"),
        )
    }

    @Test
    fun `replaces reserved file name characters`() {
        assertEquals(
            "my_video_1_.mp4",
            ArgumentSanitizer.sanitizeFileName("my:video<1>.mp4"),
        )
    }

    @Test
    fun `truncates an overly long file name`() {
        val sanitized = ArgumentSanitizer.sanitizeFileName("a".repeat(500))

        assertEquals(180, sanitized?.length)
    }

    @Test
    fun `accepts a legitimate format expression`() {
        assertEquals(
            "bestvideo[height<=?1080]+bestaudio/best",
            ArgumentSanitizer.sanitizeFormatExpression("bestvideo[height<=?1080]+bestaudio/best"),
        )
    }

    @Test
    fun `rejects a format expression containing a shell metacharacter`() {
        assertNull(ArgumentSanitizer.sanitizeFormatExpression("best;rm -rf /"))
        assertNull(ArgumentSanitizer.sanitizeFormatExpression("best\$(id)"))
        assertNull(ArgumentSanitizer.sanitizeFormatExpression("-f"))
    }

    @Test
    fun `normalises a valid header`() {
        assertEquals(
            "User-Agent: Zdon/1.0",
            ArgumentSanitizer.sanitizeHeader("  User-Agent:Zdon/1.0 "),
        )
    }

    @Test
    fun `rejects header injection attempts`() {
        assertNull(ArgumentSanitizer.sanitizeHeader("X-Evil: a\r\nHost: evil.example"))
        assertNull(ArgumentSanitizer.sanitizeHeader("Bad Header Name: value"))
        assertNull(ArgumentSanitizer.sanitizeHeader("NoSeparator"))
        assertNull(ArgumentSanitizer.sanitizeHeader(":novalue"))
    }

    @Test
    fun `accepts a playlist item selector`() {
        assertEquals("1-5,8,10::2", ArgumentSanitizer.sanitizePlaylistItems("1-5,8,10::2"))
    }

    @Test
    fun `rejects an invalid playlist item selector`() {
        assertNull(ArgumentSanitizer.sanitizePlaylistItems("1;rm"))
        assertNull(ArgumentSanitizer.sanitizePlaylistItems("-1"))
    }

    @Test
    fun `accepts a language list`() {
        assertEquals("en,es,pt-BR", ArgumentSanitizer.sanitizeLanguageList("en,es,pt-BR"))
        assertEquals("all", ArgumentSanitizer.sanitizeLanguageList("all"))
    }

    @Test
    fun `rejects an invalid language list`() {
        assertNull(ArgumentSanitizer.sanitizeLanguageList("en;rm -rf /"))
    }
}

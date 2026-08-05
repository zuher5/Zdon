package com.zdon.core.common.url

import com.zdon.core.model.SupportedSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies URL validation, including the injection-resistant character policy that
 * protects the yt-dlp argument list.
 */
class UrlValidatorTest {

    @Test
    fun `accepts a full https url and identifies the site`() {
        val result = UrlValidator.validate("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertTrue(result is UrlValidationResult.Valid)
        result as UrlValidationResult.Valid
        assertEquals(SupportedSite.YOUTUBE, result.site)
    }

    @Test
    fun `upgrades a bare host to https`() {
        val result = UrlValidator.validate("youtu.be/dQw4w9WgXcQ")

        assertTrue(result is UrlValidationResult.Valid)
        result as UrlValidationResult.Valid
        assertTrue(result.url.startsWith("https://"))
        assertEquals(SupportedSite.YOUTUBE, result.site)
    }

    @Test
    fun `trims surrounding whitespace`() {
        val result = UrlValidator.validate("   https://vimeo.com/22439234   ")

        assertTrue(result is UrlValidationResult.Valid)
        assertEquals("https://vimeo.com/22439234", (result as UrlValidationResult.Valid).url)
    }

    @Test
    fun `rejects an empty value`() {
        val result = UrlValidator.validate("   ")

        assertEquals(UrlError.EMPTY, (result as UrlValidationResult.Invalid).error)
    }

    @Test
    fun `rejects a non http scheme`() {
        val result = UrlValidator.validate("ftp://example.com/video.mp4")

        assertEquals(UrlError.UNSUPPORTED_SCHEME, (result as UrlValidationResult.Invalid).error)
    }

    @Test
    fun `rejects a value that would be parsed as a yt-dlp option`() {
        val result = UrlValidator.validate("--exec=rm -rf /")

        assertEquals(UrlError.MALFORMED, (result as UrlValidationResult.Invalid).error)
    }

    @Test
    fun `rejects shell metacharacters`() {
        listOf(
            "https://example.com/a;rm -rf /",
            "https://example.com/a\$(whoami)",
            "https://example.com/a|cat",
            "https://example.com/a&&id",
            "https://example.com/a`id`",
        ).forEach { candidate ->
            val result = UrlValidator.validate(candidate)
            assertTrue(
                "Expected $candidate to be rejected",
                result is UrlValidationResult.Invalid,
            )
        }
    }

    @Test
    fun `rejects embedded newlines`() {
        val result = UrlValidator.validate("https://example.com/a\nhttps://evil.example")

        assertEquals(UrlError.ILLEGAL_CHARACTERS, (result as UrlValidationResult.Invalid).error)
    }

    @Test
    fun `rejects a host without a dot`() {
        val result = UrlValidator.validate("https://localhost/video")

        assertEquals(UrlError.MALFORMED, (result as UrlValidationResult.Invalid).error)
    }

    @Test
    fun `accepts a supported host that has no branded label`() {
        val result = UrlValidator.validate("https://example.com/media.mp4")

        assertTrue(result is UrlValidationResult.Valid)
        assertNull((result as UrlValidationResult.Valid).site)
    }

    @Test
    fun `detects playlist style links`() {
        assertTrue(UrlValidator.looksLikePlaylist("https://youtube.com/playlist?list=PL123"))
        assertTrue(UrlValidator.looksLikePlaylist("https://soundcloud.com/user/sets/mix"))
        assertFalse(UrlValidator.looksLikePlaylist("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `resolves known hosts including subdomains`() {
        assertEquals(SupportedSite.TIKTOK, SupportedSite.fromHost("vm.tiktok.com"))
        assertEquals(SupportedSite.TWITTER, SupportedSite.fromHost("x.com"))
        assertEquals(SupportedSite.REDDIT, SupportedSite.fromHost("v.redd.it"))
        assertEquals(SupportedSite.FACEBOOK, SupportedSite.fromHost("www.facebook.com"))
        assertNull(SupportedSite.fromHost("notasite.invalid"))
    }
}

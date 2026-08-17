package com.zdon.core.engine

import com.zdon.core.model.DownloadErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorClassifierTest {

    @Test
    fun `blank message reports a network error when offline`() {
        assertEquals(DownloadErrorType.NETWORK, ErrorClassifier.classify(null, isOnline = false))
        assertEquals(DownloadErrorType.NETWORK, ErrorClassifier.classify("  ", isOnline = false))
    }

    @Test
    fun `blank message is unknown when online`() {
        assertEquals(DownloadErrorType.UNKNOWN, ErrorClassifier.classify(null))
        assertEquals(DownloadErrorType.UNKNOWN, ErrorClassifier.classify(null, isOnline = true))
    }

    @Test
    fun `classifies missing ffmpeg before anything more generic`() {
        assertEquals(
            DownloadErrorType.FFMPEG_MISSING,
            ErrorClassifier.classify("ERROR: ffmpeg not found. Please install ffmpeg."),
        )
    }

    @Test
    fun `classifies disk full`() {
        assertEquals(
            DownloadErrorType.DISK_FULL,
            ErrorClassifier.classify("ERROR: [Errno 28] No space left on device"),
        )
    }

    @Test
    fun `classifies not found messages`() {
        assertEquals(
            DownloadErrorType.NOT_FOUND,
            ErrorClassifier.classify("ERROR: HTTP Error 404: Not Found"),
        )
        assertEquals(
            DownloadErrorType.NOT_FOUND,
            ErrorClassifier.classify("Video unavailable. The uploader has deleted this video."),
        )
    }

    @Test
    fun `classifies captcha and age confirmed messages as media specific`() {
        assertEquals(
            DownloadErrorType.CAPTCHA_REQUIRED,
            ErrorClassifier.classify("Sign in to confirm you're not a bot."),
        )
        assertEquals(
            DownloadErrorType.AGE_RESTRICTED,
            ErrorClassifier.classify("Sign in to confirm your age."),
        )
    }

    @Test
    fun `specific cookies signature wins over generic forbidden`() {
        // Signature ordering: COOKIES_EXPIRED is tested before FORBIDDEN, so a
        // message that matches both must not be downgraded to a plain 403.
        val message = "ERROR: The provided cookies are not valid; Access denied for this account"
        assertEquals(DownloadErrorType.COOKIES_EXPIRED, ErrorClassifier.classify(message))
    }

    @Test
    fun `classifies geo restricted`() {
        assertEquals(
            DownloadErrorType.GEO_RESTRICTED,
            ErrorClassifier.classify("This video is not available in your country."),
        )
    }

    @Test
    fun `extract primary message strips the error prefix`() {
        assertEquals(
            "Private video. Sign in if you've been granted access to this video.",
            ErrorClassifier.extractPrimaryMessage(
                "WARNING: [youtube] Something\nERROR: Private video. " +
                    "Sign in if you've been granted access to this video.\n  File: traceback",
            ),
        )
    }

    @Test
    fun `extract primary message falls back to the last non warning line`() {
        val message = ErrorClassifier.extractPrimaryMessage(
            listOf("#2: WARNING: skipped", "Signed in using cookies").joinToString("\n"),
        )
        assertTrue(message!!.startsWith("Signed in using cookies"))
    }

    @Test
    fun `extract primary message truncates long output`() {
        val value = ErrorClassifier.extractPrimaryMessage("ERROR: " + "x".repeat(1_000))
        assertEquals(400, value?.length)
    }

    @Test
    fun `extract primary message tolerates blank input`() {
        assertNull(ErrorClassifier.extractPrimaryMessage(null))
        assertNull(ErrorClassifier.extractPrimaryMessage(""))
    }
}
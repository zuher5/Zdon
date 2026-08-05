package com.zdon.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YtDlpVersionTest {

    @Test
    fun `parses a full date version`() {
        assertEquals(YtDlpVersion(2025, 11, 12), YtDlpVersion.parse("2025.11.12"))
    }

    @Test
    fun `parses the version line printed by yt-dlp --version`() {
        assertEquals(YtDlpVersion(2025, 11, 12), YtDlpVersion.parse("2025.11.12\n"))
    }

    @Test
    fun `parses single digit month and day`() {
        assertEquals(YtDlpVersion(2023, 3, 4), YtDlpVersion.parse("2023.3.4"))
    }

    @Test
    fun `ignores a nightly build suffix`() {
        assertEquals(YtDlpVersion(2023, 6, 3), YtDlpVersion.parse("2023.6.3.63406"))
    }

    @Test
    fun `compares versions numerically instead of lexically`() {
        assertTrue(YtDlpVersion(2025, 11, 12) > YtDlpVersion(2025, 4, 5))
        assertTrue(YtDlpVersion(2024, 12, 31) < YtDlpVersion(2025, 1, 1))
    }

    @Test
    fun `same version is equal`() {
        assertEquals(0, YtDlpVersion(2025, 1, 1).compareTo(YtDlpVersion(2025, 1, 1)))
    }

    @Test
    fun `rejects garbage input`() {
        assertNull(YtDlpVersion.parse("not a version"))
        assertNull(YtDlpVersion.parse(""))
        assertNull(YtDlpVersion.parse(null))
        assertNull(YtDlpVersion.parse("2.3.4"))
    }
}

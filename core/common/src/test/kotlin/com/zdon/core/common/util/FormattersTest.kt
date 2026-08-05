package com.zdon.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    @Test
    fun `formats byte sizes with the expected unit`() {
        assertEquals("0 B", Formatters.formatBytes(0L))
        assertEquals("512 B", Formatters.formatBytes(512L))
        assertEquals("1.0 KB", Formatters.formatBytes(1024L))
        assertEquals("1.5 MB", Formatters.formatBytes(1_572_864L))
        assertEquals("2.0 GB", Formatters.formatBytes(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun `returns null for an unknown size`() {
        assertNull(Formatters.formatBytesOrNull(0L))
        assertNull(Formatters.formatBytesOrNull(-1L))
    }

    @Test
    fun `formats transfer speed`() {
        assertEquals("1.0 MB/s", Formatters.formatSpeedOrNull(1_048_576L))
        assertNull(Formatters.formatSpeedOrNull(0L))
    }

    @Test
    fun `formats durations under and over an hour`() {
        assertEquals("00:45", Formatters.formatDuration(45L))
        assertEquals("12:33", Formatters.formatDuration(753L))
        assertEquals("1:12:33", Formatters.formatDuration(4_353L))
    }

    @Test
    fun `clamps a negative duration`() {
        assertEquals("00:00", Formatters.formatDuration(-10L))
    }

    @Test
    fun `abbreviates large counts`() {
        assertEquals("999", Formatters.formatCountOrNull(999L))
        assertEquals("1.5K", Formatters.formatCountOrNull(1_500L))
        assertEquals("2.3M", Formatters.formatCountOrNull(2_300_000L))
        assertEquals("1.2B", Formatters.formatCountOrNull(1_200_000_000L))
        assertNull(Formatters.formatCountOrNull(null))
    }

    @Test
    fun `formats and clamps percentages`() {
        assertEquals("42%", Formatters.formatPercent(42.4f))
        assertEquals("100%", Formatters.formatPercent(180f))
        assertEquals("0%", Formatters.formatPercent(-5f))
    }
}

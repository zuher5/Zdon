package com.zdon.core.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YtDlpCapabilitiesTest {

    private val recent = YtDlpVersion(2025, 11, 12)

    @Test
    fun `unknown capabilities allow every documented option`() {
        val capabilities = YtDlpCapabilities.unknown()
        assertTrue(capabilities.supports("--format"))
        assertTrue(capabilities.supports("--progress-template"))
        assertTrue(capabilities.supports("--color"))
        assertTrue(capabilities.supports("-f"))
    }

    @Test
    fun `unknown capabilities reject options that are not documented`() {
        val capabilities = YtDlpCapabilities.unknown()
        assertFalse(capabilities.supports("--no-part-file-warning"))
        assertFalse(capabilities.supports("--definitely-not-an-option"))
        assertFalse(capabilities.supports("--format-sort=ext"))
    }

    @Test
    fun `help output is the ground truth for availability`() {
        val capabilities = YtDlpCapabilities(
            version = recent,
            availableOptions = setOf("--format", "--no-colors", "--color"),
        )
        assertTrue(capabilities.supports("--format"))
        assertTrue(capabilities.supports("--color"))
        assertFalse(capabilities.supports("--progress-template"))
    }

    @Test
    fun `recent version supports options introduced since 2023`() {
        val capabilities = YtDlpCapabilities(version = recent)
        assertTrue(capabilities.supports("--color"))
        assertTrue(capabilities.supports("--progress"))
        assertTrue(capabilities.supports("--progress-template"))
    }

    @Test
    fun `old version omits recently introduced options`() {
        val capabilities = YtDlpCapabilities(version = YtDlpVersion(2021, 1, 1))
        assertFalse(capabilities.supports("--progress"))
        assertFalse(capabilities.supports("--progress-template"))
        assertFalse(capabilities.supports("--color"))
    }

    @Test
    fun `old version keeps the legacy no-colors fallback`() {
        val capabilities = YtDlpCapabilities(version = YtDlpVersion(2021, 1, 1))
        assertTrue(capabilities.supports("--no-colors"))
        assertTrue(capabilities.supports("-f"))
        assertTrue(capabilities.supports("-o"))
    }

    @Test
    fun `unknown version with help output follows the help output`() {
        val capabilities = YtDlpCapabilities(
            version = null,
            availableOptions = setOf("--format", "--output"),
        )
        assertTrue(capabilities.supports("--format"))
        assertFalse(capabilities.supports("--color"))
    }
}

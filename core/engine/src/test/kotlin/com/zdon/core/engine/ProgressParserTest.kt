package com.zdon.core.engine

import com.zdon.core.model.DownloadProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers both output formats the engine feeds to [ProgressParser]: the
 * delimited template emitted by our own `--progress-template` and the
 * human-readable `[download]` lines yt-dlp falls back to for post-processors.
 */
class ProgressParserTest {

    private val previous = DownloadProgress.initial(downloadId = 7)

    @Test
    fun `template line parses every field`() {
        val line = templateLine(
            percent = "42.5%",
            downloaded = "524288",
            total = "1048576",
            totalEstimate = "NA",
            speed = "102400",
            eta = "45",
            fragmentIndex = "3",
            fragmentCount = "10",
        )

        val result = ProgressParser.parse(line, previous)!!

        assertEquals(42.5f, result.percent, 0f)
        assertEquals(524_288L, result.downloadedBytes)
        assertEquals(1_048_576L, result.totalBytes)
        assertEquals(102_400L, result.speedBytesPerSecond)
        assertEquals(45L, result.etaSeconds)
        assertEquals("3/10", result.fragment)
        assertEquals(line, result.rawLine)
    }

    @Test
    fun `template line falls back to the total estimate`() {
        val line = templateLine(
            percent = "10%",
            downloaded = "1000",
            total = "NA",
            totalEstimate = "2048",
            speed = "NA",
            eta = "NA",
            fragmentIndex = "NA",
            fragmentCount = "NA",
        )

        val result = ProgressParser.parse(line, previous)!!

        assertEquals(1_000L, result.downloadedBytes)
        assertEquals(2_048L, result.totalBytes)
        // Missing speed/eta reset to zero/unknown rather than stale values.
        assertEquals(0L, result.speedBytesPerSecond)
        assertEquals(-1L, result.etaSeconds)
    }

    @Test
    fun `template missing fragment keeps the previous fragment`() {
        val line = templateLine(
            percent = "5%",
            downloaded = "100",
            total = "200",
            totalEstimate = "NA",
            speed = "NA",
            eta = "NA",
            fragmentIndex = "NA",
            fragmentCount = "NA",
        )
        val withFragment = previous.copy(fragment = "1/2")

        val result = ProgressParser.parse(line, withFragment)!!

        assertEquals("1/2", result.fragment)
    }

    @Test
    fun `human readable line parses percent total speed and eta`() {
        val line = "[download]  42.5% of 1.00MiB at 100.00KiB/s ETA 00:45"

        val result = ProgressParser.parse(line, previous)!!

        assertEquals(42.5f, result.percent, 0f)
        assertEquals(1_048_576L, result.totalBytes)
        // 1 MiB * 42.5% truncated to whole bytes.
        assertEquals(445_644L, result.downloadedBytes)
        assertEquals(102_400L, result.speedBytesPerSecond)
        assertEquals(45L, result.etaSeconds)
    }

    @Test
    fun `human readable line reports fragment progress`() {
        val line = "[download]  45.0% of 2.00MiB (frag 5/12)"

        val result = ProgressParser.parse(line, previous)!!

        assertEquals(45f, result.percent, 0f)
        assertEquals("5/12", result.fragment)
    }

    @Test
    fun `percent is clamped to one hundred`() {
        val line = "[download]  110.0% of 1.00MiB"

        val result = ProgressParser.parse(line, previous)!!

        assertEquals(100f, result.percent, 0f)
    }

    @Test
    fun `non progress lines are ignored`() {
        assertNull(ProgressParser.parse("", previous))
        assertNull(ProgressParser.parse("[download] Destination: song.mp3", previous))
        assertNull(ProgressParser.parse("[youtube] Extracting URL: https://example.com", previous))
    }

    @Test
    fun `destination lines expose the output path`() {
        assertEquals(
            "/data/media/file.mp4",
            ProgressParser.parseDestination("[download] Destination: /data/media/file.mp4"),
        )
        assertEquals(
            "merged.mkv",
            ProgressParser.parseDestination("""[Merger] Merging formats into "merged.mkv""""),
        )
        assertEquals(
            "cached.mp4",
            ProgressParser.parseDestination(
                "[download] /home/user/cached.mp4 has already been downloaded",
            ),
        )
    }

    private fun templateLine(
        percent: String,
        downloaded: String,
        total: String,
        totalEstimate: String,
        speed: String,
        eta: String,
        fragmentIndex: String,
        fragmentCount: String,
    ): String = listOf(
        ProgressParser.TEMPLATE_PREFIX,
        percent,
        downloaded,
        total,
        totalEstimate,
        speed,
        eta,
        fragmentIndex,
        fragmentCount,
    ).joinToString(ProgressParser.FIELD_SEPARATOR)
}
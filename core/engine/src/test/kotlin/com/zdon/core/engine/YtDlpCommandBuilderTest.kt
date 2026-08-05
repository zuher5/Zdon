package com.zdon.core.engine

import com.yausername.youtubedl_android.YoutubeDLRequest
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.VideoQuality
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that every command produced by [YtDlpCommandBuilder] only contains
 * options supported by the documented yt-dlp option set, and that options which
 * are unavailable for the detected version are omitted.
 */
class YtDlpCommandBuilderTest {

    private val builder = YtDlpCommandBuilder()

    private val recentCapabilities = YtDlpCapabilities(version = YtDlpVersion(2025, 11, 12))

    @Test
    fun `download command only contains documented options`() {
        val command = builder
            .buildDownloadRequest(
                fullRequest(),
                fullOptions(),
                YtDlpCapabilities.unknown(),
            )
            .buildCommand()

        assertOptionsDocumented(command)
    }

    @Test
    fun `info command only contains documented options`() {
        val command = builder
            .buildInfoRequest(
                url = "https://example.com/video",
                options = fullOptions(),
                flatPlaylist = true,
                capabilities = YtDlpCapabilities.unknown(),
            )
            .buildCommand()

        assertOptionsDocumented(command)
    }

    @Test
    fun `command never contains the removed no-part-file-warning option`() {
        val command = builder
            .buildDownloadRequest(
                fullRequest(),
                fullOptions(),
                YtDlpCapabilities.unknown(),
            )
            .buildCommand()

        assertFalse(command.contains("--no-part-file-warning"))
    }

    @Test
    fun `modern version emits color as --color no_color instead of deprecated --no-colors`() {
        val command = builder
            .buildDownloadRequest(
                fullRequest(),
                fullOptions(),
                recentCapabilities,
            )
            .buildCommand()

        assertTrue(command.contains("--color"))
        assertTrue(command[command.indexOf("--color") + 1] == "no_color")
        assertFalse(command.contains("--no-colors"))
    }

    @Test
    fun `old version falls back to legacy --no-colors and drops --color`() {
        val oldCapabilities = YtDlpCapabilities(version = YtDlpVersion(2021, 1, 1))
        val command = builder
            .buildDownloadRequest(
                fullRequest(),
                fullOptions(),
                oldCapabilities,
            )
            .buildCommand()

        assertTrue(command.contains("--no-colors"))
        assertFalse(command.contains("--color"))
    }

    @Test
    fun `old version omits progress options introduced in 2021_10`() {
        val oldCapabilities = YtDlpCapabilities(version = YtDlpVersion(2021, 1, 1))
        val command = builder
            .buildDownloadRequest(
                fullRequest(),
                fullOptions(),
                oldCapabilities,
            )
            .buildCommand()

        assertFalse(command.contains("--progress-template"))
        assertFalse(command.contains("--progress"))
        // --newline is ancient and must survive.
        assertTrue(command.contains("--newline"))
    }

    @Test
    fun `options absent from help output are omitted`() {
        val capabilities = YtDlpCapabilities(
            version = YtDlpVersion(2025, 11, 12),
            availableOptions = setOf(
                "--format", "--no-cache-dir", "--ignore-config", "--no-mtime",
                "--proxy", "--cookies", "--add-headers", "--download-archive",
                "--restrict-filenames", "--newline", "--continue", "--retries",
                "--fragment-retries", "--socket-timeout", "--no-abort-on-error",
                "--merge-output-format", "--extract-audio", "--audio-format",
                "--audio-quality", "--embed-metadata", "--write-subs",
                "--write-auto-subs", "--sub-langs", "--embed-subs",
                "--write-thumbnail", "--embed-thumbnail", "--yes-playlist",
                "--no-playlist", "--output", "--paths",
            ),
        )
        val command = builder
            .buildDownloadRequest(
                fullRequest(),
                fullOptions(),
                capabilities,
            )
            .buildCommand()

        // --progress-template is missing from the parsed help output.
        assertFalse(command.contains("--progress-template"))
        assertFalse(command.contains("--color"))
        assertTrue(command.contains("-f"))
    }

    @Test
    fun `audio extraction command uses documented postprocessing options`() {
        val request = fullRequest().copy(
            extractAudio = true,
            audioFormat = AudioFormat.MP3,
        )
        val command = builder
            .buildDownloadRequest(request, fullOptions(), recentCapabilities)
            .buildCommand()

        assertOptionsDocumented(command)
        assertTrue(command.contains("--extract-audio"))
        assertTrue(command.contains("--audio-format"))
        assertTrue(command.contains("--audio-quality"))
    }

    @Test
    fun `subtitle and thumbnail options are documented`() {
        val request = fullRequest().copy(
            downloadSubtitles = true,
            subtitleLanguages = listOf("en", "es"),
            embedSubtitles = true,
            downloadThumbnail = true,
            embedThumbnail = true,
            embedMetadata = true,
        )
        val command = builder
            .buildDownloadRequest(request, fullOptions(), recentCapabilities)
            .buildCommand()

        assertOptionsDocumented(command)
        assertTrue(command.contains("--embed-metadata"))
        assertFalse(command.contains("--add-metadata"))
    }

    @Test
    fun `custom format selector is emitted`() {
        val request = fullRequest().copy(
            quality = VideoQuality.CUSTOM,
            customFormatId = "bestvideo[height<=?1080]+bestaudio",
        )
        val command = builder
            .buildDownloadRequest(request, fullOptions(), recentCapabilities)
            .buildCommand()

        val formatIndex = command.indexOf("-f")
        assertTrue(formatIndex >= 0)
        assertTrue(command[formatIndex + 1] == "bestvideo[height<=?1080]+bestaudio")
    }

    private fun assertOptionsDocumented(command: List<String>) {
        command.forEach { token ->
            if (token.startsWith("-")) {
                assertTrue("Unexpected yt-dlp option: $token", YtDlpOptions.isDocumented(token))
            }
        }
    }

    private fun fullOptions(): EngineOptions = EngineOptions(
        workingDirectoryPath = "/data/work",
        temporaryDirectoryPath = "/data/tmp",
        outputTemplate = "%(title)s.%(ext)s",
        retries = 3,
        proxyUrl = "socks5://127.0.0.1:1080",
        cookiesFilePath = "/data/cookies.txt",
        customHeaders = listOf("User-Agent: Mozilla/5.0"),
        downloadArchivePath = "/data/archive.txt",
        useDownloadArchive = true,
        restrictFilenames = true,
    )

    private fun fullRequest(): DownloadRequest = DownloadRequest(
        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        title = "Test video",
        quality = VideoQuality.FHD_1080P,
        audioFormat = AudioFormat.ORIGINAL,
        extractAudio = false,
        downloadSubtitles = false,
        embedMetadata = false,
        downloadThumbnail = false,
        isPlaylist = false,
    )
}

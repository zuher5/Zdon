package com.zdon.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaInfoParserTest {

    @Test
    fun `single video parses metadata and selected format fields`() {
        val info = MediaInfoParser.parse(singleVideoJson(), "https://example.com/v")

        assertEquals("abc123", info.id)
        assertEquals("https://example.com/v", info.originalUrl)
        assertEquals("Test Video", info.title)
        assertEquals("The Channel", info.uploader)
        assertEquals(245L, info.durationSeconds)
        assertEquals("https://example.com/high.png", info.thumbnailUrl)
        assertFalse(info.isPlaylist)
        assertTrue(info.entries.isEmpty())

        assertEquals(listOf("en", "id"), info.subtitleLanguages)

        assertEquals(1, info.formats.size)
        val format = info.formats.single()
        assertEquals("18", format.formatId)
        assertEquals("mp4", format.extension)
        assertEquals(1280, format.width)
        assertEquals(720, format.height)
        assertEquals("avc1", format.videoCodec)
        assertEquals(128, format.audioBitrateKbps)

        // filesize_approx is used when the exact size is unknown.
        assertEquals(9_999L, format.fileSizeBytes)
        assertTrue(format.isApproximateSize)
    }

    @Test
    fun `mhtml pseudo formats are skipped`() {
        val json = singleVideoJson()
            .replace("\"protocol\": \"https\"", "\"protocol\": \"mhtml\"")
        val info = MediaInfoParser.parse(json, "https://example.com/v")
        assertTrue(info.formats.isEmpty())
    }

    @Test
    fun `playlist enumerates its entries recursively`() {
        val info = MediaInfoParser.parse(playlistJson(), "https://example.com/playlist")

        assertTrue(info.isPlaylist)
        assertEquals("My Playlist", info.title)
        assertEquals(2, info.entries.size)
        assertEquals("First", info.entries[0].title)
        assertEquals("https://youtu.be/1", info.entries[0].webpageUrl)
        assertEquals(1, info.entries[0].formats.size)
        assertEquals(6_000L, info.entries[0].formats.single().fileSizeBytes)
    }

    @Test
    fun `garbage tolerant parsing stays stable`() {
        val info = MediaInfoParser.parse("{}", "https://example.com/x")

        assertEquals("Untitled", info.title)
        assertNull(info.uploader)
        assertEquals(0L, info.durationSeconds)
        assertTrue(info.formats.isEmpty())
        assertFalse(info.isPlaylist)
    }

    private fun singleVideoJson(): String = """
        {
          "id": "abc123",
          "webpage_url": "https://example.com/v",
          "title": "Test Video",
          "channel": "The Channel",
          "duration": 245,
          "view_count": 1000,
          "thumbnails": [
            {"url": "https://example.com/low.png", "width": 320, "height": 180},
            {"url": "https://example.com/high.png", "width": 1280, "height": 720}
          ],
          "subtitles": {"en": [{}], "id": [{}]},
          "formats": [
            {
              "format_id": "18",
              "ext": "mp4",
              "protocol": "https",
              "format_note": "360p",
              "width": 1280,
              "height": 720,
              "vcodec": "avc1",
              "acodec": "mp4a",
              "vbr": 1000.0,
              "abr": 128.0,
              "filesize_approx": 9999
            },
            {"format_id": "sb0", "ext": "mhtml", "protocol": "mhtml"}
          ]
        }
    """.trimIndent()

    private fun playlistJson(): String = """
        {
          "_type": "playlist",
          "id": "PL1",
          "title": "My Playlist",
          "entries": [
            {
              "id": "v1",
              "webpage_url": "https://youtu.be/1",
              "title": "First",
              "duration": 120,
              "formats": [
                {
                  "format_id": "18",
                  "ext": "mp4",
                  "protocol": "https",
                  "vcodec": "avc1",
                  "acodec": "mp4a",
                  "filesize": 6000
                }
              ]
            },
            {"id": "v2", "webpage_url": "https://youtu.be/2", "title": "Second"}
          ]
        }
    """.trimIndent()
}
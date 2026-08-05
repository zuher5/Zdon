package com.zdon.core.engine

import com.zdon.core.model.MediaFormat
import com.zdon.core.model.MediaInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the JSON produced by `yt-dlp --dump-single-json` into [MediaInfo].
 *
 * `org.json` is used instead of the library's bundled Jackson mapper because the
 * mapper classes expose only a subset of the fields Zdon shows (bitrates,
 * subtitles, playlist entries), and because parsing here keeps the engine free
 * of a second JSON dependency.
 */
internal object MediaInfoParser {

    fun parse(json: String, originalUrl: String): MediaInfo {
        val root = JSONObject(json)
        return parseObject(root, originalUrl)
    }

    private fun parseObject(root: JSONObject, originalUrl: String): MediaInfo {
        val type = root.optStringOrNull("_type")
        val entriesArray = root.optJSONArray("entries")
        val isPlaylist = type == "playlist" || type == "multi_video" || entriesArray != null

        val entries = buildList {
            if (entriesArray != null) {
                for (index in 0 until entriesArray.length()) {
                    val entry = entriesArray.optJSONObject(index) ?: continue
                    add(parseObject(entry, entry.optStringOrNull("webpage_url") ?: originalUrl))
                }
            }
        }

        return MediaInfo(
            id = root.optStringOrNull("id").orEmpty(),
            originalUrl = originalUrl,
            webpageUrl = root.optStringOrNull("webpage_url"),
            title = root.optStringOrNull("title")
                ?: root.optStringOrNull("fulltitle")
                ?: UNKNOWN_TITLE,
            uploader = root.optStringOrNull("uploader")
                ?: root.optStringOrNull("channel")
                ?: root.optStringOrNull("uploader_id"),
            durationSeconds = root.optDoubleOrNull("duration")?.toLong() ?: 0L,
            viewCount = root.optLongOrNull("view_count"),
            likeCount = root.optLongOrNull("like_count"),
            thumbnailUrl = root.optStringOrNull("thumbnail") ?: bestThumbnail(root),
            description = root.optStringOrNull("description"),
            extractor = root.optStringOrNull("extractor_key") ?: root.optStringOrNull("extractor"),
            uploadDate = root.optStringOrNull("upload_date"),
            formats = parseFormats(root.optJSONArray("formats")),
            subtitleLanguages = parseSubtitleLanguages(root),
            isPlaylist = isPlaylist,
            entries = entries,
        )
    }

    private fun parseFormats(array: JSONArray?): List<MediaFormat> {
        if (array == null) return emptyList()
        val formats = ArrayList<MediaFormat>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val formatId = item.optStringOrNull("format_id") ?: continue
            // Storyboard/manifest-only pseudo formats cannot be downloaded directly.
            val protocol = item.optStringOrNull("protocol").orEmpty()
            if (protocol == "mhtml") continue

            val fileSize = item.optLongOrNull("filesize")
            val approximateSize = item.optLongOrNull("filesize_approx")
            formats += MediaFormat(
                formatId = formatId,
                extension = item.optStringOrNull("ext").orEmpty(),
                formatNote = item.optStringOrNull("format_note")
                    ?: item.optStringOrNull("resolution"),
                width = item.optIntOrNull("width"),
                height = item.optIntOrNull("height"),
                fps = item.optDoubleOrNull("fps")?.toInt(),
                videoCodec = item.optStringOrNull("vcodec"),
                audioCodec = item.optStringOrNull("acodec"),
                videoBitrateKbps = item.optDoubleOrNull("vbr")?.toInt(),
                audioBitrateKbps = item.optDoubleOrNull("abr")?.toInt(),
                totalBitrateKbps = item.optDoubleOrNull("tbr")?.toInt(),
                fileSizeBytes = fileSize ?: approximateSize,
                isApproximateSize = fileSize == null && approximateSize != null,
            )
        }
        return formats
    }

    private fun parseSubtitleLanguages(root: JSONObject): List<String> {
        val languages = LinkedHashSet<String>()
        listOf("subtitles", "automatic_captions").forEach { key ->
            val container = root.optJSONObject(key) ?: return@forEach
            container.keys().forEach { language ->
                if (language.isNotBlank()) languages += language
            }
        }
        return languages.toList()
    }

    private fun bestThumbnail(root: JSONObject): String? {
        val thumbnails = root.optJSONArray("thumbnails") ?: return null
        var bestUrl: String? = null
        var bestArea = -1L
        for (index in 0 until thumbnails.length()) {
            val item = thumbnails.optJSONObject(index) ?: continue
            val url = item.optStringOrNull("url") ?: continue
            val width = item.optIntOrNull("width") ?: 0
            val height = item.optIntOrNull("height") ?: 0
            val area = width.toLong() * height.toLong()
            if (area >= bestArea) {
                bestArea = area
                bestUrl = url
            }
        }
        return bestUrl
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() && it != "null" && it != "NA" }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value.toLong()
    }

    private fun JSONObject.optIntOrNull(key: String): Int? = optLongOrNull(key)?.toInt()

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value
    }

    private const val UNKNOWN_TITLE = "Untitled"
}

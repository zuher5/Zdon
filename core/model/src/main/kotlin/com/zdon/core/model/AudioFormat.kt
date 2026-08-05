package com.zdon.core.model

/** Container/codec for audio-only downloads and for MP3 conversion. */
enum class AudioFormat(val extension: String, val label: String) {
    /** Keep whatever the site served; no re-encode, fastest and lossless. */
    ORIGINAL("", "Original"),
    MP3("mp3", "MP3"),
    M4A("m4a", "M4A / AAC"),
    OPUS("opus", "Opus"),
    VORBIS("vorbis", "Vorbis"),
    FLAC("flac", "FLAC"),
    WAV("wav", "WAV"),
    ;

    /** Whether selecting this format forces an FFmpeg transcode. */
    val requiresFfmpeg: Boolean
        get() = this != ORIGINAL

    companion object {
        fun fromNameOrDefault(name: String?, fallback: AudioFormat = MP3): AudioFormat =
            entries.firstOrNull { it.name == name } ?: fallback
    }
}

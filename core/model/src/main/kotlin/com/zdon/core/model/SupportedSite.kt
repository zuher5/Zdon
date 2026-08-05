package com.zdon.core.model

/**
 * Supported extractor families. Used purely for URL validation and for
 * labelling entries in the UI; extraction itself is always delegated to yt-dlp
 * so newly supported sites keep working after a binary update.
 */
enum class SupportedSite(
    val displayName: String,
    val hostSuffixes: List<String>,
) {
    YOUTUBE("YouTube", listOf("youtube.com", "youtu.be", "youtube-nocookie.com", "music.youtube.com")),
    TIKTOK("TikTok", listOf("tiktok.com", "vm.tiktok.com", "vt.tiktok.com")),
    INSTAGRAM("Instagram", listOf("instagram.com", "instagr.am", "ddinstagram.com")),
    FACEBOOK("Facebook", listOf("facebook.com", "fb.watch", "fb.com", "m.facebook.com")),
    TWITTER("Twitter / X", listOf("twitter.com", "x.com", "t.co", "mobile.twitter.com")),
    REDDIT("Reddit", listOf("reddit.com", "redd.it", "v.redd.it", "old.reddit.com")),
    VIMEO("Vimeo", listOf("vimeo.com", "player.vimeo.com")),
    DAILYMOTION("Dailymotion", listOf("dailymotion.com", "dai.ly")),
    SOUNDCLOUD("SoundCloud", listOf("soundcloud.com", "snd.sc", "on.soundcloud.com")),
    ;

    companion object {
        /**
         * Resolves a [SupportedSite] from a host name, matching the host itself
         * and any subdomain of a known suffix. Returns `null` for unknown hosts;
         * those are still downloadable because yt-dlp supports far more sites,
         * they simply have no branded label.
         */
        fun fromHost(host: String?): SupportedSite? {
            if (host.isNullOrBlank()) return null
            val normalized = host.lowercase().removePrefix("www.")
            return entries.firstOrNull { site ->
                site.hostSuffixes.any { suffix ->
                    normalized == suffix || normalized.endsWith(".$suffix")
                }
            }
        }
    }
}

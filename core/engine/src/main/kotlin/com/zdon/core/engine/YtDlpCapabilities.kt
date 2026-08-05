package com.zdon.core.engine

/**
 * The option set accepted by the installed yt-dlp binary, plus the detected
 * version.
 *
 * Three signals are combined, in order of decreasing reliability:
 *
 * 1. [availableOptions] - the long-form option names parsed from the binary's
 *    own `--help` output. This is the ground truth for the binary actually on
 *    the device.
 * 2. [version] - used to gate options whose introduction is a documented fact,
 *    so a binary older than the introducing release drops the option instead
 *    of crashing.
 * 3. The official documentation whitelist in [YtDlpOptions] - used when the
 *    binary could not be probed, so a missing probe never breaks downloads.
 */
data class YtDlpCapabilities(
    val version: YtDlpVersion? = null,
    val availableOptions: Set<String>? = null,
) {

    /**
     * True when [option] may be passed to the detected yt-dlp binary.
     *
     * An option is supported when its canonical name is a documented option,
     * the detected version is not older than the option's introduction, and -
     * when the binary's `--help` was parsed - the option appears in it.
     */
    fun supports(option: String): Boolean {
        val canonical = YtDlpOptions.canonicalName(option)
        if (!YtDlpOptions.isDocumented(canonical)) return false

        val minimumVersion = YtDlpOptions.MINIMUM_VERSIONS[canonical]
        if (minimumVersion != null && version != null && version < minimumVersion) return false

        return availableOptions == null || canonical in availableOptions
    }

    companion object {

        /**
         * Capabilities used when the installed binary could not be probed.
         * Every documented option is assumed to be available.
         */
        fun unknown(): YtDlpCapabilities = YtDlpCapabilities()

        /**
         * Capabilities built from a successful probe of the installed binary.
         */
        fun detected(
            version: YtDlpVersion?,
            availableOptions: Set<String>?,
        ): YtDlpCapabilities = YtDlpCapabilities(version, availableOptions)
    }
}

package com.zdon.app.logging

import android.util.Log
import timber.log.Timber

/**
 * Release logging tree.
 *
 * Verbose and debug logs are dropped so the release build never leaks URLs or
 * cookie paths into logcat. Warnings and above are kept because they are needed
 * to diagnose extractor failures from a bug report. Every write is wrapped so a
 * logging failure can never crash the app - the whole point of a crash-safe tree.
 */
class CrashSafeReleaseTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val safeTag = tag ?: DEFAULT_TAG
            val trimmed = message.take(MAX_MESSAGE_LENGTH)
            if (t != null) {
                Log.println(priority, safeTag, "$trimmed\n${Log.getStackTraceString(t)}")
            } else {
                Log.println(priority, safeTag, trimmed)
            }
        } catch (_: Throwable) {
            // Intentionally swallowed: logging must never take the process down.
        }
    }

    private companion object {
        const val DEFAULT_TAG = "Zdon"
        const val MAX_MESSAGE_LENGTH = 4_000
    }
}

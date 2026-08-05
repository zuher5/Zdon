package com.zdon.core.downloader.storage

import android.content.Context
import android.net.Uri
import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies a user-selected cookies file (a SAF content URI) into app-private
 * storage so yt-dlp, which requires a filesystem path, can read it.
 *
 * The private copy is refreshed on every download so an updated export in the
 * source file is picked up without re-selecting it.
 */
@Singleton
class CookieFileProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(ZdonDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Materialises [cookiesUriString] as a local file.
     *
     * @return the absolute path, or `null` when no cookies are configured or the
     * URI is no longer readable.
     */
    suspend fun materialize(cookiesUriString: String?): String? = withContext(ioDispatcher) {
        if (cookiesUriString.isNullOrBlank()) return@withContext null
        val uri = runCatching { Uri.parse(cookiesUriString) }.getOrNull()
            ?: return@withContext null

        val target = File(context.filesDir, COOKIES_FILE)
        try {
            target.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            
            if (isCookieFileExpired(target)) {
                Timber.w("Cookies file appears to be expired")
            }
            
            target.absolutePath
        } catch (exception: IOException) {
            Timber.e(exception, "Unable to read cookies file")
            null
        } catch (exception: SecurityException) {
            Timber.e(exception, "Cookies file permission lost")
            null
        }
    }
    
    private fun isCookieFileExpired(file: File): Boolean {
        if (!file.exists()) return true
        val lines = file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) return true
        
        val currentTime = System.currentTimeMillis() / 1000
        return lines.any { line ->
            val parts = line.split("\t")
            if (parts.size >= 5) {
                val expiry = parts[4].toLongOrNull() ?: 0
                expiry > 0 && expiry < currentTime
            } else false
        }
    }

    private companion object {
        const val COOKIES_FILE = "cookies.txt"
    }
}

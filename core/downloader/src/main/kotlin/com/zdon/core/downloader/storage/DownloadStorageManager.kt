package com.zdon.core.downloader.storage

import android.content.ContentResolver
import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.StatFs
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges yt-dlp (which needs a real filesystem path) and the Storage Access
 * Framework (which hands out content URIs).
 *
 * Strategy: every download is written to an app-private staging directory, then
 * published to the user's chosen SAF tree with [publish]. This keeps the app
 * fully scoped-storage compliant on Android 10+ (including 13/14/15/16), works
 * for internal storage, SD cards and cloud providers alike, and needs no
 * `WRITE_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE` permission.
 */
@Singleton
class DownloadStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(ZdonDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    /** Private directory yt-dlp writes into. Created on demand. */
    fun stagingDirectory(downloadId: Long): File =
        File(context.filesDir, "$STAGING_ROOT/$downloadId").apply { mkdirs() }

    /** Private directory for `.part` fragments and post-processing scratch files. */
    fun temporaryDirectory(downloadId: Long): File =
        File(context.cacheDir, "$TEMP_ROOT/$downloadId").apply { mkdirs() }

    /** Path of the yt-dlp download archive, or `null` when the feature is off. */
    fun downloadArchiveFile(enabled: Boolean): File? =
        if (!enabled) {
            null
        } else {
            File(context.filesDir, ARCHIVE_FILE).apply {
                parentFile?.mkdirs()
                if (!exists()) runCatching { createNewFile() }
            }
        }

    /** Removes the staging and temporary directories for a finished download. */
    suspend fun clearWorkspace(downloadId: Long) = withContext(ioDispatcher) {
        stagingDirectory(downloadId).deleteRecursivelyQuietly()
        temporaryDirectory(downloadId).deleteRecursivelyQuietly()
    }

    /**
     * Deletes every staging and temporary directory whose download id is not in
     * [knownIds]. Called on startup and whenever rows are deleted, so files
     * orphaned by a failed or removed download cannot accumulate in app-private
     * storage.
     */
    suspend fun clearOrphanedWorkspaces(knownIds: Set<Long>) = withContext(ioDispatcher) {
        val roots = listOf(
            File(context.filesDir, STAGING_ROOT),
            File(context.cacheDir, TEMP_ROOT),
        )
        roots.forEach { root ->
            if (!root.isDirectory) return@forEach
            root.listFiles().orEmpty().forEach { child ->
                val id = child.name.toLongOrNull()
                if (id == null || id !in knownIds) {
                    child.deleteRecursivelyQuietly()
                }
            }
        }
    }

    /**
     * Persists read/write access to a SAF tree so the folder keeps working after
     * a reboot. Returns `true` when the grant was taken successfully.
     */
    fun persistTreePermission(treeUri: Uri): Boolean = try {
        resolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        true
    } catch (exception: SecurityException) {
        Timber.e(exception, "Unable to persist permission for %s", treeUri)
        false
    }

    /** Releases a previously persisted grant; safe to call for unknown URIs. */
    fun releaseTreePermission(treeUri: Uri) {
        runCatching {
            resolver.releasePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.onFailure { Timber.w(it, "Unable to release permission for %s", treeUri) }
    }

    /** True when the app still holds a writable grant for [treeUriString]. */
    fun canWriteTo(treeUriString: String?): Boolean {
        val uri = treeUriString?.toUriOrNull() ?: return false
        val hasGrant = resolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
        if (!hasGrant) return false
        return DocumentFile.fromTreeUri(context, uri)?.canWrite() == true
    }

    /** Human-readable folder name for the settings screen. */
    fun displayName(treeUriString: String?): String? {
        val uri = treeUriString?.toUriOrNull() ?: return null
        return DocumentFile.fromTreeUri(context, uri)?.name
            ?: DocumentsContract.getTreeDocumentId(uri)
    }

    /**
     * Copies every file produced by yt-dlp from the staging directory into the
     * user's chosen folder and deletes the staging copies.
     *
     * @return the published primary file, or a [PublishResult.Failure] describing
     * why publication was not possible.
     */
    suspend fun publish(
        downloadId: Long,
        treeUriString: String?,
        primaryFileName: String?,
    ): PublishResult = withContext(ioDispatcher) {
        val staging = stagingDirectory(downloadId)
        val produced = staging.walkTopDown()
            .filter { it.isFile && it.length() > 0L && !it.name.endsWith(PART_SUFFIX) }
            .toList()

        if (produced.isEmpty()) {
            return@withContext PublishResult.Failure(PublishError.NOTHING_PRODUCED)
        }

        val treeUri = treeUriString?.toUriOrNull()
            ?: return@withContext PublishResult.Failure(PublishError.NO_DESTINATION)
        val tree = DocumentFile.fromTreeUri(context, treeUri)
        if (tree == null || !tree.canWrite()) {
            return@withContext PublishResult.Failure(PublishError.PERMISSION_DENIED)
        }

        val requiredBytes = produced.sumOf { it.length() }
        if (!hasFreeSpaceFor(requiredBytes)) {
            return@withContext PublishResult.Failure(PublishError.DISK_FULL)
        }

        var primary: PublishedFile? = null
        try {
            produced.forEach { file ->
                val published = copyToTree(tree, file)
                if (primary == null || file.name == primaryFileName) {
                    primary = published
                }
            }
        } catch (exception: IOException) {
            Timber.e(exception, "Publishing download %d failed", downloadId)
            return@withContext PublishResult.Failure(exception.toPublishError())
        } catch (exception: SecurityException) {
            Timber.e(exception, "Publishing download %d denied", downloadId)
            return@withContext PublishResult.Failure(PublishError.PERMISSION_DENIED)
        }

        staging.deleteRecursivelyQuietly()
        primary
            ?.let { PublishResult.Success(it) }
            ?: PublishResult.Failure(PublishError.NOTHING_PRODUCED)
    }

    private fun copyToTree(tree: DocumentFile, source: File): PublishedFile {
        val mimeType = MimeTypes.fromFileName(source.name)
        val existing = tree.findFile(source.name)
        // Replacing keeps re-downloads idempotent instead of piling up "(1)" copies.
        existing?.takeIf { it.isFile }?.delete()

        val target = tree.createFile(mimeType, source.name)
            ?: throw IOException("Unable to create ${source.name} in the download folder")

        resolver.openOutputStream(target.uri, "w")?.use { output ->
            source.inputStream().use { input -> input.copyTo(output, COPY_BUFFER_BYTES) }
        } ?: throw FileNotFoundException("Unable to open ${target.uri} for writing")

        val size = source.length()
        source.delete()
        return PublishedFile(
            uri = target.uri.toString(),
            displayName = target.name ?: source.name,
            sizeBytes = size,
        )
    }

    private fun hasFreeSpaceFor(bytes: Long): Boolean = try {
        val stat = StatFs(context.filesDir.absolutePath)
        stat.availableBytes > bytes + FREE_SPACE_HEADROOM_BYTES
    } catch (exception: IllegalArgumentException) {
        Timber.w(exception, "Unable to read free space")
        true
    }

    private fun IOException.toPublishError(): PublishError {
        val message = message?.lowercase().orEmpty()
        return when {
            message.contains("enospc") || message.contains("no space") -> PublishError.DISK_FULL
            message.contains("permission") -> PublishError.PERMISSION_DENIED
            else -> PublishError.IO_ERROR
        }
    }

    private fun File.deleteRecursivelyQuietly() {
        runCatching { deleteRecursively() }
            .onFailure { Timber.w(it, "Unable to delete %s", absolutePath) }
    }

    private fun String.toUriOrNull(): Uri? =
        runCatching { Uri.parse(this) }.getOrNull()?.takeIf { it.scheme != null }

    private companion object {
        const val STAGING_ROOT = "downloads/staging"
        const val TEMP_ROOT = "downloads/temp"
        const val ARCHIVE_FILE = "downloads/archive.txt"
        const val PART_SUFFIX = ".part"
        const val COPY_BUFFER_BYTES = 128 * 1024
        const val FREE_SPACE_HEADROOM_BYTES = 16L * 1024L * 1024L
    }
}

/** A file that now lives in the user's chosen folder. */
data class PublishedFile(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
)

/** Outcome of [DownloadStorageManager.publish]. */
sealed interface PublishResult {
    data class Success(val file: PublishedFile) : PublishResult
    data class Failure(val error: PublishError) : PublishResult
}

/** Why publishing failed. Mapped to a user-facing message by the caller. */
enum class PublishError {
    NO_DESTINATION,
    PERMISSION_DENIED,
    DISK_FULL,
    NOTHING_PRODUCED,
    IO_ERROR,
}

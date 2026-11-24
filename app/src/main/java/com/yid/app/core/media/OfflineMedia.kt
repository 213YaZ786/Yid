package com.yid.app.core.media

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.yid.app.core.model.MediaItem
import com.yid.app.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Where automatically saved media lives, so a post can be watched with no
 * network.
 *
 * Under the app's own external files directory, not Downloads. Downloads is
 * the reader's own drawer and forty pictures a day in it is vandalism. Here
 * the app owns the folder: no permission to ask for, nothing offered to the
 * gallery, and the whole thing disappears when the app is uninstalled, which
 * is the correct fate for a cache.
 *
 * The index is the file name and there is no database. A name carries the
 * handle, the post id and the position of the media within that post, which
 * is everything needed to go from a post on screen to a file on disk and
 * back. A database for that would be a second copy of the truth, free to
 * disagree with the folder.
 *
 * A video is saved with its still (its cover, from the item's previewUrl), so the post shows its picture offline too.
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#OfflineMedia
 * not ported: getMediafilePath, FileNameGenerator and the folder per account (LinkedOut's AntennaPod naming, Yiḍ keeps the
 * handle in the file name: SavedMedia and Settings read it there)
 * not ported: parseFile and files() (their only job is the account folders)
 * not ported: the deletion of the flat files written before the account folders (Yiḍ never wrote any)
 * not ported: deleteWhere (nothing here calls it, SavedMediaViewModel deletes by entries())
 */
class OfflineMedia(context: Context) {

    /** Kept for DownloadManager, which [refresh] asks which transfers are still going. */
    private val appContext: Context = context.applicationContext ?: context

    /**
     * getExternalFilesDir can be null when external storage is not mounted.
     * filesDir then, which is smaller but always there: a reader with no SD
     * card should still get offline playback.
     */
    private val directory =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "media").apply { mkdirs() }

    private val _version = MutableStateFlow(0)

    /**
     * Counts every change of what is saved, a file landing or going, so a
     * screen that is open reads the index again at once instead of at its next
     * resume. Compose keys on it, see rememberMediaSource.
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#version
     */
    val version: StateFlow<Int> = _version.asStateFlow()

    /** Where [item] of [postId] is, or would be. Never null, may not exist. */
    fun fileFor(postId: String, authorHandle: String, index: Int, item: MediaItem): File =
        File(directory, name(postId, authorHandle, index, item))

    /**
     * Where the still of a video of [postId] is, or would be. Same scheme as [fileFor].
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#stillFor
     */
    fun stillFor(postId: String, authorHandle: String, index: Int, item: MediaItem): File =
        File(directory, stillName(postId, authorHandle, index, item))

    /**
     * postId and position to absolute path, for everything on disk.
     *
     * Held in memory because the lookup happens once per picture per frame of
     * a scrolling list, and listing a directory there would cost more than
     * the picture. Rebuilt by [refresh], never guessed.
     */
    @Volatile
    private var index: Map<String, String> = emptyMap()

    /** The same for the stills of videos. */
    @Volatile
    private var stills: Map<String, String> = emptyMap()

    /**
     * Re-reads the folder, one listing and one question to DownloadManager.
     * Bumps [version] only when something moved, so a pass that found nothing
     * new recomposes nothing.
     *
     * A file DownloadManager is still writing is no copy yet, and the part
     * file of a transfer that failed is removed with its row, otherwise the
     * downloader finds it there and never tries again.
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#refresh
     */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        val transfers = isDownloadActive()
        val listed = directory.listFiles().orEmpty().filter { it.isFile }
        val active = Companion.isDownloadActive(listed, transfers.unfinished, transfers.failed.keys, cancel = false)
        val cancelled = Companion.isDownloadActive(listed, transfers.unfinished, transfers.failed.keys, cancel = true)
        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        cancelled.forEach { file ->
            // remove() also deletes the file, the delete covers a row already gone.
            transfers.failed[file.absolutePath]?.let { id -> runCatching { manager?.remove(id) } }
            runCatching { file.delete() }
        }
        val main = HashMap<String, String>()
        val still = HashMap<String, String>()
        (listed - active.toSet() - cancelled.toSet()).forEach { file ->
            val parsed = parse(file.name) ?: return@forEach
            // A file DownloadManager has not finished writing is no copy yet.
            if (file.length() == 0L) return@forEach
            (if (parsed.still) still else main)[key(parsed.postId, parsed.index)] = file.absolutePath
        }
        if (main != index || still != stills) {
            index = main
            stills = still
            _version.update { it + 1 }
        }
    }

    /**
     * The transfers of this folder that DownloadManager has not finished: the
     * paths of those paused, pending or running, and the id of each failed one
     * by its path.
     * https://github.com/rdoeffinger/Dictionary/blob/main/src/com/hughes/android/dictionary/DictionaryManagerActivity.kt#isDownloadActive
     * Dictionary asks the same question (the same query, filtered on status)
     * for one address. Here the answer is every unfinished file of the folder,
     * read from the destination, since the names are ours and the addresses
     * are not. Dictionary's cancel flag and its alert dialogs are not ported:
     * nothing is cancelled here and a reader that cannot ask simply knows
     * nothing, see [downloadManagerQuery].
     * not ported: the match on COLUMN_URI and COLUMN_TITLE (it looks for one
     * address, here every file of the folder is wanted)
     * not ported: finishedDownloadIds (Dictionary's own bookkeeping of ids it
     * cancelled, nothing here reads it)
     */
    private fun isDownloadActive(): Transfers {
        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return Transfers()
        val query = DownloadManager.Query()
        query.setFilterByStatus(
            DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_PENDING or
                DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_FAILED
        )
        val cursor = downloadManagerQuery(downloadManager, query) ?: return Transfers()

        val unfinished = HashSet<String>()
        val failed = HashMap<String, Long>()
        val prefix = directory.absolutePath + File.separator
        cursor.use {
            val id = it.getColumnIndex(DownloadManager.COLUMN_ID)
            val status = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val localUri = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            if (id < 0 || status < 0 || localUri < 0) return Transfers()
            while (it.moveToNext()) {
                // LOCAL_URI is a file:// address, and null until the transfer has a destination.
                val path = it.getString(localUri)?.let { uri -> Uri.parse(uri).path } ?: continue
                if (!path.startsWith(prefix)) continue
                if (it.getInt(status) == DownloadManager.STATUS_FAILED) failed[path] = it.getLong(id)
                else unfinished.add(path)
            }
        }
        return Transfers(unfinished, failed)
    }

    /**
     * Runs the query. A SecurityException or a null cursor, which is what the
     * manager gives when it is disabled (a bug Dictionary notes), mean that
     * nothing is known: the caller carries on as if no transfer were running.
     * https://github.com/rdoeffinger/Dictionary/blob/main/src/com/hughes/android/dictionary/DictionaryManagerActivity.kt#downloadManagerQuery
     * not ported: showDownloadFailedAlert (this is a quiet pass at each
     * refresh, an alert for it would only get in the way)
     */
    private fun downloadManagerQuery(
        downloadManager: DownloadManager,
        query: DownloadManager.Query?
    ): Cursor? {
        val cursor: Cursor?
        try {
            cursor = downloadManager.query(query)
        } catch (se: SecurityException) {
            Log.w(LOG, "Failed to query download manager", se)
            return null
        }
        return cursor
    }

    /** What DownloadManager has not finished: the paths still being written, the id of each failed one by path. */
    private data class Transfers(
        val unfinished: Set<String> = emptySet(),
        val failed: Map<String, Long> = emptyMap()
    )

    /**
     * Where this media is on disk, or null when it was never saved.
     *
     * Not suspending on purpose: it is read during composition, and a photo
     * that is already on the phone should never wait on a coroutine to be
     * shown.
     */
    fun localPath(postId: String, position: Int): String? = index[key(postId, position)]

    /**
     * The saved still of a video, or null. Same index as [localPath].
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#localStill
     */
    fun localStill(postId: String, position: Int): String? = stills[key(postId, position)]

    private fun key(postId: String, position: Int) = "$postId/$position"

    /**
     * The copies on disk, stills left out, newest file first.
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#entries
     */
    suspend fun entries(): List<Entry> = withContext(Dispatchers.IO) {
        directory.listFiles()
            .orEmpty()
            .mapNotNull { file ->
                parse(file.name)?.takeIf { !it.still }?.let { Entry(file, it.handle, it.postId, it.index) }
            }
            .sortedByDescending { it.file.lastModified() }
    }

    /**
     * The saved still of each video of the folder, by post id and position. Same listing as [refresh].
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#stillPaths
     */
    suspend fun stillPaths(): Map<String, String> = withContext(Dispatchers.IO) {
        refresh()
        stills
    }

    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        directory.listFiles().orEmpty().sumOf { it.length() }
    }

    suspend fun delete(file: File): Boolean {
        val gone = withContext(Dispatchers.IO) { runCatching { file.delete() }.getOrDefault(false) }
        if (gone) refresh()
        return gone
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            directory.listFiles().orEmpty().forEach { runCatching { it.delete() } }
        }
        refresh()
    }

    /**
     * Deletes the media of posts the cache no longer holds.
     *
     * Retention is set once, in "Keep posts", and it governs this too: a
     * media file outlives nothing. A second size budget to tune would be a
     * second thing to get wrong.
     */
    suspend fun keepOnly(postIds: Set<String>): Int = withContext(Dispatchers.IO) {
        var removed = 0
        directory.listFiles().orEmpty().forEach { file ->
            val parsed = parse(file.name)
            if (parsed == null || parsed.postId !in postIds) {
                if (runCatching { file.delete() }.getOrDefault(false)) removed++
            }
        }
        if (removed > 0) refresh()
        removed
    }

    /** One saved file, with the post it came from. */
    data class Entry(val file: File, val handle: String, val postId: String, val index: Int)

    /** What a file name says about the media it holds. A [still] is the picture of a saved video. */
    internal data class Parsed(val handle: String, val postId: String, val index: Int, val still: Boolean = false)

    companion object {
        /** The sub directory of the app's files folder, shared with the downloader. */
        const val FOLDER = "media"

        private const val STILL = "still"

        private const val LOG = "OfflineMedia"

        /**
         * Dictionary's isDownloadActive without Android, so a JVM test can run
         * it. Without [cancel] it answers which listed files a transfer is
         * still writing (paused, pending or running): they stay on disk and
         * out of the index. With [cancel] it answers which listed files belong
         * to a failed transfer: those are to be removed, their part file would
         * block any retry.
         * https://github.com/rdoeffinger/Dictionary/blob/main/src/com/hughes/android/dictionary/DictionaryManagerActivity.kt#isDownloadActive
         */
        internal fun isDownloadActive(files: List<File>, active: Set<String>, failed: Set<String>, cancel: Boolean): List<File> {
            val downloads = if (cancel) failed else active
            return files.filter { destFile -> destFile.absolutePath in downloads }
        }

        /**
         * handle, then post id, then position within the post, then
         * extension. The separator is a double underscore because a handle
         * can contain a single one.
         *
         * This name is the whole index. Pure, so the round trip with [parse]
         * can be tested off Android, which is the only thing standing between
         * a saved file and the post that owns it.
         */
        internal fun name(postId: String, authorHandle: String, index: Int, item: MediaItem): String {
            val extension = when (item.type) {
                MediaType.PHOTO -> extensionOf(item.downloadUrl, "jpg")
                else -> extensionOf(item.downloadUrl, "mp4")
            }
            return "${authorHandle.lowercase()}__${baseName(postId)}__$index.$extension"
        }

        /**
         * The name of the still of a video: [name]'s parts with a "still" part
         * and a picture extension.
         * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#stillName
         */
        internal fun stillName(postId: String, authorHandle: String, index: Int, item: MediaItem): String =
            "${authorHandle.lowercase()}__${baseName(postId)}__${index}__$STILL.jpg"

        /**
         * A Bluesky post id is an at-URI, slashes and colons that no file name
         * can hold, and the index key is the id itself, so the part is the id
         * encoded and decoded whole by [parse], never stripped.
         * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#baseName
         * not ported: FileNameGenerator.generateFileName (it drops characters, the id could not come back)
         */
        private fun baseName(postId: String): String {
            var baseFilename = URLEncoder.encode(postId, "UTF-8")
            val filenameMaxLength = 220
            if (baseFilename.length > filenameMaxLength) {
                baseFilename = baseFilename.substring(0, filenameMaxLength)
            }
            return baseFilename
        }

        /**
         * What a file name says about its media: a name of three parts, or of four with "still" last.
         * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/core/media/OfflineMedia.kt#parse
         */
        internal fun parse(fileName: String): Parsed? {
            val parts = fileName.substringBeforeLast('.').split("__")
            if (parts.size != 3 && !(parts.size == 4 && parts[3] == STILL)) return null
            val index = parts[2].toIntOrNull() ?: return null
            if (parts[0].isEmpty() || parts[1].isEmpty()) return null
            val postId = runCatching { URLDecoder.decode(parts[1], "UTF-8") }.getOrNull() ?: return null
            return Parsed(parts[0], postId, index, still = parts.size == 4)
        }

        private fun extensionOf(url: String, fallback: String): String {
            val tail = url.substringBefore('?').substringAfterLast('.', "")
            return if (tail.length in 2..4 && tail.all { it.isLetterOrDigit() }) tail else fallback
        }
    }
}

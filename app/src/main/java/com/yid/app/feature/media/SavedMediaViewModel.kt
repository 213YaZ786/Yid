package com.yid.app.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yid.app.core.media.OfflineMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Ported from LinkedOut's SavedMediaViewModel
// (https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#SavedMediaViewModel).
// Yiḍ's OfflineMedia stays a folder read by file name: it has no version flow, no stills and no deleteWhere, so what
// served them is left out and the deletes go file by file through delete().
// not ported: offline.version collect in init (OfflineMedia keeps no version: the grid reads again after each delete and on open)
// not ported: deleteAll (the validated picture has no Delete all, and OfflineMedia.clear stays where Settings uses it)
// not ported: offline.stillPaths (no video stills are kept: a video's cell draws the plain play mark)

// Yiḍ's own earlier model, no longer read by the screen: SavedMediaDraftScreenshotTest still draws today's picture
// ("saved_media_now") from it, which stays as the proof of what the point changed.
/** One post's saved files. */
data class SavedMediaPost(val postId: String, val files: Int, val bytes: Long)

/** One account's saved files, newest post first. */
data class SavedMediaGroup(
    val handle: String,
    val files: Int,
    val bytes: Long,
    val posts: List<SavedMediaPost>
)

/** One line of the screen's grid: an account's heading or one saved media. */
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#SavedRow
sealed interface SavedRow {
    val key: String
}

/** An account's heading, with how many files it has saved and their size. */
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#SavedHeader
data class SavedHeader(
    val handle: String,
    val files: Int,
    val bytes: Long
) : SavedRow {
    override val key: String get() = "h/$handle"
}

/** A post's line above its cells: how many files, to open it or delete it. */
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#SavedPostBar
data class SavedPostBar(
    val postId: String,
    val files: Int,
    val bytes: Long
) : SavedRow {
    override val key: String get() = "p/$postId"
}

/**
 * One saved media. [thumb] is a picture to draw at the cell's size: the file of
 * a photo, the still of a video, null when a video has no still (the cell then
 * draws a plain play mark, no video frame is ever decoded for it).
 */
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#SavedCell
data class SavedCell(
    val postId: String,
    val position: Int,
    val thumb: String?,
    val video: Boolean
) : SavedRow {
    override val key: String get() = "c/$postId/$position"
}

// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#SavedMediaUiState
data class SavedMediaUiState(
    val loading: Boolean = true,
    val rows: List<SavedRow> = emptyList(),
    val fileCount: Int = 0,
    val totalBytes: Long = 0
)

/**
 * Reads the folder and builds the rows of a lazy grid: thousands
 * of saved videos are one list of small rows, built off the main thread, and
 * only the cells in view ever draw anything.
 *
 * There is no index to keep in step because the file names are the index, so
 * a file the reader deleted by hand simply stops appearing.
 */
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#SavedMediaViewModel
class SavedMediaViewModel(private val offline: OfflineMedia) : ViewModel() {

    private val _state = MutableStateFlow(SavedMediaUiState())
    val state: StateFlow<SavedMediaUiState> = _state.asStateFlow()

    init {
        loadImages()
    }

    /** The post's copies in the app's folder. */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            offline.entries().filter { it.postId == postId }.forEach { offline.delete(it.file) }
            reload()
        }
    }

    fun deleteAccount(handle: String) {
        viewModelScope.launch {
            offline.entries().filter { it.handle == handle }.forEach { offline.delete(it.file) }
            reload()
        }
    }

    /** Loads the saved media into [state]. */
    fun loadImages() {
        viewModelScope.launch { reload() }
    }

    /** The folder read once and built into the rows. */
    private suspend fun reload() {
        val entries = offline.entries()
        val built = withContext(Dispatchers.Default) {
            val sized = entries.map { Auto(it.handle, it.postId, it.index, it.file.path, it.file.length()) }
            build(sized, emptyMap())
        }
        _state.value = built.copy(loading = false)
    }

    /** One file of the app's folder with its size, read once. */
    internal data class Auto(val handle: String, val postId: String, val position: Int, val path: String, val bytes: Long)

    internal companion object {
        private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mov", "webm", "mkv")

        /**
         * The rows of the grid, pure. Accounts with the most files first, then
         * each account's posts newest first (a record key is a TID, so a bigger
         * key is a later post), the media of a post in order. [stills] are
         * keyed "postId/position" like the store's own index.
         */
        // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaViewModel.kt#build
        fun build(auto: List<Auto>, stills: Map<String, String>): SavedMediaUiState {
            class Item(val handle: String, val cell: SavedCell, val bytes: Long)

            val items = LinkedHashMap<String, Item>()
            for (file in auto) {
                val video = file.path.substringAfterLast('.').lowercase() in VIDEO_EXTENSIONS
                val thumb = if (video) stills["${file.postId}/${file.position}"] else file.path
                val cell = SavedCell(file.postId, file.position, thumb, video)
                items[cell.key] = Item(file.handle, cell, file.bytes)
            }

            val rows = ArrayList<SavedRow>(items.size + 16)
            items.values.groupBy { it.handle }
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, List<Item>>> { it.value.size }.thenBy { it.key })
                .forEach { (handle, ofAccount) ->
                    rows += SavedHeader(handle, ofAccount.size, ofAccount.sumOf { it.bytes })
                    ofAccount
                        .sortedWith(
                            compareByDescending<Item> { it.cell.postId.length }
                                .thenByDescending { it.cell.postId }
                                .thenBy { it.cell.position }
                        )
                        .groupBy { it.cell.postId }
                        .forEach { (postId, ofPost) ->
                            rows += SavedPostBar(postId, ofPost.size, ofPost.sumOf { it.bytes })
                            ofPost.forEach { rows += it.cell }
                        }
                }
            return SavedMediaUiState(
                loading = false,
                rows = rows,
                fileCount = items.size,
                totalBytes = items.values.sumOf { it.bytes }
            )
        }
    }
}

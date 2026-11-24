package com.yid.app.screenshot

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.di.appModule
import com.yid.app.feature.media.SavedMediaGroup
import com.yid.app.feature.media.SavedMediaPost
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.BarTitle
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.BoldIconButton
import com.yid.app.ui.component.FloatingTopBar
import com.yid.app.ui.component.QuietButton
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.component.navigationBarBottom
import com.yid.app.ui.component.plus
import com.yid.app.ui.icon.YidIcons
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.absoluteValue

/**
 * DRAFT for the user's yes (requests.md point 19), nothing of app/src/main changes: Saved media as a grid by account.
 * saved_media_now is today's screen (feature/media/SavedMediaScreen.kt: a bar with Back, the total, then one zone per
 * account with its "Delete all" and, per post, a text button "N files, size" and a bin, no picture). saved_media_draft is
 * the grid LinkedOut built from MTGA's: the total at the top, per account a header with its "Delete copies", per post a line
 * with its file count and size and its bin, the pictures as cells in a LazyVerticalGrid with GridCells.Adaptive, a video
 * with its play mark, under the FloatingTopBar.
 *
 * Ports: the screen, its grid and its row composables (AccountHeader, PostBar, Cell), the row types and the pure build of
 * the rows, from LinkedOut's draft of the same point (validated by the user):
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SavedMediaDraftScreenshotTest.kt#SavedMediaDraftScreenshotTest
 * Today's screen is Yiḍ's own: its groups and its AccountGroup are copied from it
 * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/media/SavedMediaScreen.kt#SavedMediaScreen
 * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/media/SavedMediaViewModel.kt#SavedMediaGroup
 *
 * not ported: LinkedOut's `appBytes` line "in the app's folder" (Yiḍ keeps every saved file in its own folder, the total
 * says "N files, size" as today's does), SavedMediaViewModel's init, deletePost, deleteAccount, reload and the content
 * observer (a picture has fixed data), `build`'s second loop over the manual saves (Yiḍ has no saves made by hand), the
 * empty and loading text, the posts' open callbacks (nothing is tapped), AsyncImage for the cell's picture (Robolectric
 * loads no network or file picture, so a box in the theme's container colours stands in: the app draws the saved file with
 * Coil at the cell's size), and the Palette of fixed colours (the readers take their colours from the phone's palette).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class SavedMediaDraftScreenshotTest : ScreenshotBase() {

    @Before
    fun startKoinOnce() {
        startKoin {
            androidContext(compose.activity.applicationContext)
            modules(appModule)
        }
    }

    @After
    fun stopKoinAgain() = stopKoin()

    // Two accounts, 2 and 3 posts, a video with a still and a video without, photos in between. Sizes in bytes.
    private val files = listOf(
        Auto("ada.bsky.social", "3kdraft0013", 0, "/files/media/ada.bsky.social/3kdraft0013_0.jpg", 612_000),
        Auto("ada.bsky.social", "3kdraft0013", 1, "/files/media/ada.bsky.social/3kdraft0013_1.jpg", 488_000),
        Auto("ada.bsky.social", "3kdraft0013", 2, "/files/media/ada.bsky.social/3kdraft0013_2.jpg", 530_000),
        Auto("ada.bsky.social", "3kdraft0012", 0, "/files/media/ada.bsky.social/3kdraft0012_0.mp4", 8_400_000),
        Auto("ada.bsky.social", "3kdraft0011", 0, "/files/media/ada.bsky.social/3kdraft0011_0.jpg", 745_000),
        Auto("ada.bsky.social", "3kdraft0011", 1, "/files/media/ada.bsky.social/3kdraft0011_1.jpg", 390_000),
        Auto("bram.example.com", "3kdraft0022", 0, "/files/media/bram.example.com/3kdraft0022_0.jpg", 902_000),
        Auto("bram.example.com", "3kdraft0022", 1, "/files/media/bram.example.com/3kdraft0022_1.mp4", 12_800_000),
        Auto("bram.example.com", "3kdraft0021", 0, "/files/media/bram.example.com/3kdraft0021_0.jpg", 455_000)
    )

    // The still of the first video only: the second shows the plain play mark, as a video without a still does.
    private val stills = mapOf("3kdraft0012/0" to "/files/media/stills/3kdraft0012_0.jpg")

    // Read at first use: build needs VIDEO_EXTENSIONS, declared further down and still null at this point of the class.
    private val state by lazy { build(files, stills) }

    // Today's groups, as SavedMediaViewModel's reload builds them: the account's files and bytes, its posts' too.
    private val groups by lazy {
        files.groupBy { it.handle }.map { (handle, ofAccount) ->
            SavedMediaGroup(
                handle = handle,
                files = ofAccount.size,
                bytes = ofAccount.sumOf { it.bytes },
                posts = ofAccount.groupBy { it.postId }.map { (postId, ofPost) ->
                    SavedMediaPost(postId, ofPost.size, ofPost.sumOf { it.bytes })
                }
            )
        }
    }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SavedMediaDraftScreenshotTest.kt#savedMediaNow
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun savedMediaNow() = capture("saved_media_now") { Today() }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SavedMediaDraftScreenshotTest.kt#savedMediaDraft
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun savedMediaDraft() = capture("saved_media_draft") { Draft() }

    // Edge: the grid at 200 % text and on a 360 dp phone.
    @Test
    fun edge() = captureEdge("saved_media_draft") { Draft() }

    // Today's SavedMediaScreen: the bar with Back, the total in the accent, then a zone per account.
    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/media/SavedMediaScreen.kt#SavedMediaScreen
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Today() {
        val context = LocalContext.current
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                FloatingTopBar(
                    title = { Text("Saved media") },
                    navigationIcon = { BackTarget() }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = LocalReadableInset.current,
                    end = LocalReadableInset.current,
                    bottom = 24.dp
                ).plus(padding)
            ) {
                item(key = "total") {
                    Text(
                        "${files.size} files, " + Formatter.formatShortFileSize(context, state.totalBytes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }
                items(groups, key = { it.handle }) { group -> AccountGroup(group) }
            }
        }
    }

    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/media/SavedMediaScreen.kt#AccountGroup
    @Composable
    private fun AccountGroup(group: SavedMediaGroup) {
        val context = LocalContext.current
        ZoneSurface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("@${group.handle}", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${group.files} files, " + Formatter.formatShortFileSize(context, group.bytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BoldButton(onClick = {}) { Text("Delete all") }
                }
                group.posts.forEach { post ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuietButton(onClick = {}) {
                            Text(
                                "${post.files} file${if (post.files == 1) "" else "s"}, " +
                                    Formatter.formatShortFileSize(context, post.bytes)
                            )
                        }
                        BoldIconButton(onClick = {}) {
                            Icon(YidIcons.Delete, contentDescription = "Delete this post's media")
                        }
                    }
                }
            }
        }
    }

    // The grid screen: the total, per account a SavedHeader, per post a SavedPostBar, the pictures as SavedCells.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SavedMediaDraftScreenshotTest.kt#savedMediaDraft
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Draft() {
        val context = LocalContext.current
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                FloatingTopBar(
                    title = { BarTitle("Saved media") },
                    navigationIcon = { BackTarget() }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(CELL_MIN),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = LocalReadableInset.current + 16.dp,
                        end = LocalReadableInset.current + 16.dp,
                        bottom = 24.dp + navigationBarBottom()
                    ).plus(padding),
                    horizontalArrangement = Arrangement.spacedBy(GAP),
                    verticalArrangement = Arrangement.spacedBy(GAP)
                ) {
                    item(key = "total", span = { GridItemSpan(maxLineSpan) }, contentType = "total") {
                        Text(
                            "${state.fileCount} files, " + Formatter.formatShortFileSize(context, state.totalBytes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        )
                    }

                    items(
                        state.rows,
                        key = { it.key },
                        span = { row ->
                            if (row is SavedCell) GridItemSpan(1) else GridItemSpan(maxLineSpan)
                        },
                        contentType = {
                            when (it) {
                                is SavedHeader -> "header"
                                is SavedPostBar -> "post"
                                is SavedCell -> "cell"
                            }
                        }
                    ) { row ->
                        when (row) {
                            is SavedHeader -> AccountHeader(row) {}
                            is SavedPostBar -> PostBar(row, {}) {}
                            is SavedCell -> Cell(row) {}
                        }
                    }
                }
            }
        }
    }

    // The bar's Back: a transparent Surface of 48 dp, what IconButton is (the guard refuses it in app code).
    @Composable
    private fun BackTarget() {
        Surface(onClick = {}, shape = CircleShape, color = Color.Transparent) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(YidIcons.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp))
            }
        }
    }

    /** An account's name and count above its cells, with the delete of its copies. */
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SavedMediaDraftScreenshotTest.kt#AccountHeader
    @Composable
    private fun AccountHeader(header: SavedHeader, onDelete: () -> Unit) {
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("@${header.handle}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${header.files} files, " + Formatter.formatShortFileSize(context, header.bytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BoldButton(onClick = onDelete) { Text("Delete copies") }
        }
    }

    /** A post's line: its file count opens the post, the bin deletes its copies in the app's folder. */
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SavedMediaDraftScreenshotTest.kt#PostBar
    @Composable
    private fun PostBar(bar: SavedPostBar, onOpen: () -> Unit, onDelete: () -> Unit) {
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuietButton(onClick = onOpen) {
                Text(
                    "${bar.files} file${if (bar.files == 1) "" else "s"}, " + Formatter.formatShortFileSize(context, bar.bytes)
                )
            }
            BoldIconButton(onClick = onDelete) {
                Icon(YidIcons.Delete, contentDescription = "Delete this post's media")
            }
        }
    }

    /**
     * One saved media: a square, as wide as the grid gives, no text. The picture is a box in the theme's container colours
     * here, where the app draws it with Coil at the cell's size.
     */
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SavedMediaDraftScreenshotTest.kt#Cell
    @Composable
    private fun Cell(cell: SavedCell, onOpen: () -> Unit) {
        val colours = MaterialTheme.colorScheme
        val stand = listOf(colours.primaryContainer, colours.secondaryContainer, colours.tertiaryContainer)[
            (cell.postId.hashCode().absoluteValue + cell.position) % 3
        ]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(colours.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (cell.thumb != null) {
                Box(Modifier.fillMaxSize().background(stand))
            }
            if (cell.video) {
                Icon(
                    YidIcons.Play,
                    contentDescription = "Video",
                    tint = if (cell.thumb != null) colours.onSurface else colours.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    private val CELL_MIN = 104.dp
    // SMS's TileGap, as MTGA keeps it: 4 dp let the cells touch.
    private val GAP = 10.dp

    /** One line of the screen's grid: an account's heading or one saved media. */
    private sealed interface SavedRow {
        val key: String
    }

    /** An account's heading. */
    private data class SavedHeader(
        val handle: String,
        val files: Int,
        val bytes: Long
    ) : SavedRow {
        override val key: String get() = "h/$handle"
    }

    /** A post's line above its cells: how many files, to open it or delete it. */
    private data class SavedPostBar(
        val postId: String,
        val files: Int,
        val bytes: Long
    ) : SavedRow {
        override val key: String get() = "p/$postId"
    }

    /** One saved media. [thumb] is a picture to draw at the cell's size, null when a video has no still. */
    private data class SavedCell(
        val postId: String,
        val position: Int,
        val thumb: String?,
        val video: Boolean
    ) : SavedRow {
        override val key: String get() = "c/$postId/$position"
    }

    private data class SavedMediaUiState(
        val loading: Boolean = true,
        val rows: List<SavedRow> = emptyList(),
        val fileCount: Int = 0,
        val totalBytes: Long = 0
    )

    /** One file of the app's folder with its size, read once. */
    private data class Auto(val handle: String, val postId: String, val position: Int, val path: String, val bytes: Long)

    private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mov", "webm", "mkv")

    /**
     * The rows of the grid, pure. Accounts with the most files first, then each account's posts newest first (a record
     * key is a TID, so a bigger key is a later post), the media of a post in order. [stills] are keyed "postId/position"
     * like the store's own index.
     */
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/SavedMediaDraftScreenshotTest.kt#build
    private fun build(auto: List<Auto>, stills: Map<String, String>): SavedMediaUiState {
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

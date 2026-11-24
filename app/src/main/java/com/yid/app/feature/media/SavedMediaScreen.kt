package com.yid.app.feature.media

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yid.app.feature.BarAction
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.BarTitle
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.BoldIconButton
import com.yid.app.ui.component.FloatingTopBar
import com.yid.app.ui.component.QuietButton
import com.yid.app.ui.component.navigationBarBottom
import com.yid.app.ui.component.plus
import com.yid.app.ui.icon.YidIcons
import org.koin.androidx.compose.koinViewModel

/**
 * What the app has saved so posts can be read with no network.
 *
 * Ported from LinkedOut's SavedMediaScreen
 * (https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaScreen.kt#SavedMediaScreen).
 * A lazy grid of small pictures, an account's heading above its media: with thousands of saved videos only the
 * cells in view exist, each picture is decoded at the size of its cell (Coil reads the size from the layout), and
 * a video shows the plain play mark, never a frame decoded for the grid. A tap on a post's row or a picture opens
 * the post.
 *
 * The copies sit in the app's own folder, which Android 11 and later keep out of the Files app. The account's
 * "Delete copies" and the post's bin delete part of it.
 * not ported: the Delete all action of the bar and its confirmation dialog (the validated picture has none)
 * not ported: Crossfade of the reading, empty and grid states (point 18, the animations)
 * not ported: AnimatedContent of the count and the size (point 18)
 * not ported: AnimatedVisibility of the bar's action (goes with the Delete all action)
 * not ported: animateItem of the rows (point 18)
 * not ported: the "in the app's folder" line under the total (every copy is the app's own here, the picture shows one line)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedMediaScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    viewModel: SavedMediaViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // The bar folds away going down and returns going up, the grid full screen, as LinkedOut's SavedMediaScreen.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // The page's ground is painted under the whole app, see MainActivity.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        // The status and navigation bars are the grid's own, in its content padding.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FloatingTopBar(
                scrollBehavior = scrollBehavior,
                title = { BarTitle("Saved media") },
                navigationIcon = { BarAction(YidIcons.ArrowBack, "Back", onBack) }
            )
        }
    ) { padding ->
        if (state.rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (state.loading) {
                        "Reading the folder"
                    } else {
                        "Nothing saved yet. Turn on automatic saving in Settings, Media."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
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
                        is SavedHeader -> AccountHeader(row, onDelete = { viewModel.deleteAccount(row.handle) })
                        is SavedPostBar -> PostBar(
                            row,
                            onOpen = { onOpenPost(row.postId) },
                            onDelete = { viewModel.deletePost(row.postId) }
                        )
                        is SavedCell -> Cell(row, onOpen = { onOpenPost(row.postId) })
                    }
                }
            }
        }
    }
}

/** An account's name and count above its cells, with the delete of its copies. */
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaScreen.kt#AccountHeader
@Composable
private fun AccountHeader(header: SavedHeader, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
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
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaScreen.kt#PostBar
@Composable
private fun PostBar(bar: SavedPostBar, onOpen: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
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
 * One saved media. Its picture is drawn by Coil at the cell's size through the isolated decoder like every picture
 * of the app. The cell has a shape (a square, as wide as the grid gives) and no text, so nothing in it can break.
 */
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/storage/SavedMediaScreen.kt#Cell
@Composable
private fun Cell(cell: SavedCell, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.Center
    ) {
        if (cell.thumb != null) {
            AsyncImage(
                model = cell.thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (cell.video) {
            Icon(
                YidIcons.Play,
                contentDescription = "Video",
                tint = if (cell.thumb != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private val CELL_MIN = 104.dp
// SMS's TileGap (PersonActions.kt), the gap Connection check and Folders took: 4 dp let the cells touch.
private val GAP = 10.dp

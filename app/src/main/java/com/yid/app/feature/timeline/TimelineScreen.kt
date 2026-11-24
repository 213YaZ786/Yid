package com.yid.app.feature.timeline

import com.yid.app.data.marks.PostMarks
import androidx.compose.runtime.saveable.rememberSaveable
import com.yid.app.ui.component.FailureDialog
import androidx.compose.ui.graphics.Color
import com.yid.app.ui.glass.rememberGlassBackdrop
import com.yid.app.ui.glass.glassSource
import com.yid.app.ui.glass.LocalGlassBackdrop
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.component.rememberRefreshHaptics
import com.yid.app.ui.component.LoadingMark
import com.yid.app.ui.component.PullRevealBox
import com.yid.app.ui.component.AppBarWithSearch
import com.yid.app.ui.component.BannerAction
import com.yid.app.ui.component.CapsuleMenuOffset
import com.yid.app.ui.component.DropdownMenuGroup
import com.yid.app.ui.component.FolderSwitch
import com.yid.app.ui.component.GlassMenu
import com.yid.app.ui.component.ScrollUpButton
import com.yid.app.ui.component.SearchCapsuleHint
import com.yid.app.ui.component.SelectableDropdownMenuItem
import com.yid.app.data.accounts.AccountStore
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.foundation.border
import com.yid.app.ui.component.EmptyZone
import com.yid.app.ui.component.boldBorder
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.LocalDockPadding
import com.yid.app.ui.component.LocalInlinePlaying
import com.yid.app.ui.component.rememberInlineTarget
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yid.app.data.read.ReadMarks
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.core.media.MediaDownloader
import com.yid.app.data.settings.SettingsStore
import com.yid.app.core.model.Post
import com.yid.app.feature.media.MediaViewer
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.icon.YidIcons
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Home: everything you follow in one stream, newest first.
 *
 * Pull down to refresh. A post that arrived since the last visit wears a
 * dot until you have scrolled past it. When an
 * account fails to load, a warning in the title says why.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onOpenAccounts: () -> Unit,
    onOpenPost: (Post) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: TimelineViewModel = koinViewModel()
) {
    val loaded by viewModel.state.collectAsState()
    // Liked and Archived are views picked from Home's folder menu, not
    // folders: their posts come from PostMarks, and archived posts leave the
    // other views.
    val postMarks: PostMarks = koinInject()
    val marked by postMarks.marks.collectAsState()
    var special by rememberSaveable { mutableStateOf<String?>(null) }
    val state = remember(loaded, marked, special) {
        when (special) {
            LIKED -> loaded.copy(posts = marked.liked.sortedByDescending { it.publishedAtMillis }, folder = LIKED, canLoadMore = false)
            ARCHIVED -> loaded.copy(posts = marked.archived.sortedByDescending { it.publishedAtMillis }, folder = ARCHIVED, canLoadMore = false)
            else -> if (marked.archived.isEmpty()) loaded else loaded.copy(posts = loaded.posts.filterNot { marked.isArchived(it.id) })
        }
    }
    // The threshold is felt in the indicator itself; this answers the end of
    // a refresh the reader pulled: done, or a refusal when nothing came.
    val askedRefresh = rememberRefreshHaptics(
        refreshing = state.loading,
        failed = state.errors.isNotEmpty() && (state.posts.isEmpty() || state.errors.size >= state.followedCount)
    )
    val uriHandler = LocalUriHandler.current
    val downloader: MediaDownloader = koinInject()
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settings.collectAsState()
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()
    var viewing by remember { mutableStateOf<Pair<Post, Int>?>(null) }

    viewing?.let { (post, index) ->
        MediaViewer(
            postId = post.id,
            media = post.media,
            startIndex = index,
            onDownload = { downloader.download(it, post.authorHandle, post.permalink) },
            onDismiss = { viewing = null }
        )
    }


    var showFailures by remember { mutableStateOf(false) }
    if (showFailures) {
        // The kit's FailureDialog (point 12): one centred message in the post card's glass (its default shape,
        // ShapeDefaults.ExtraLarge, as PostCard's), its title from the Blame most failed accounts share, a count, Close
        // and Try again, no account listed. Yiḍ has no silent downloads notice (LinkedIn's media progress), so false.
        // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/timeline/TimelineScreen.kt#TimelineScreen
        // not ported: silentDownloads, notifier, notificationsAllowed, somethingWrong (LinkedIn's media notice; the alert
        // is the failed accounts alone)
        // not ported: ZoneShape (LinkedOut's pane shape; Yiḍ's post card is ShapeDefaults.ExtraLarge, the dialog's default)
        FailureDialog(
            failed = state.errors.values.toList(),
            followed = state.followedCount,
            silentDownloads = false,
            site = "Bluesky",
            onRetry = {
                showFailures = false
                viewModel.refresh()
            },
            onDismiss = { showFailures = false }
        )
    }

    // The feed choice, the capsule's folder switch (requests.md points 10 and 11): the folder icon, the feed's name and a
    // chevron, which opens a menu under it: All and the folders, then Liked and Archived, the shown one checked
    // (Material 3 Expressive's grouped menu in glass, GlassMenu). Picking does what a tap on a chip did.
    // The capsule is an item of the list that records listBackdrop: floating on it, it would draw the recording that holds
    // it, a layer inside itself, and Android's renderer overflows its stack at the first frame. So AppBarWithSearch draws it
    // with no backdrop, and the menu lives in a window of its own.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/timeline/TimelineScreen.kt#TimelineScreen
    // not ported: FloatingFrame (SMS's frame; here the capsule draws itself with no backdrop)
    // not ported: onPostScroll (SMS's nested scroll of the frame; Home's list has its own)
    // not ported: TimelineScreen (the screen itself is this file's, only the feed choice, the capsule, the pull and the way
    // back to the top are rebuilt here)
    // not ported: silentDownloads, notifier, notificationsAllowed, NewPostNotifier (LinkedIn's media notice; Yiḍ's alert is
    // the failed accounts alone)
    // not ported: seen and readPosts (LinkedOut's ReadPosts; Yiḍ's ReadMarks below is its own)
    // not ported: TimelineFooter's AnimatedContent and animateItem on the footer and on the empty state (point 18, not now)
    // not ported: PullIndicator (LinkedOut's own pull; Yiḍ calls the kit's PullRevealBox, point 3)
    var feedsOpen by remember { mutableStateOf(false) }
    val showFeed: (String?) -> Unit = { name ->
        feedsOpen = false
        if (name == LIKED || name == ARCHIVED) {
            special = name
        } else {
            special = null
            viewModel.showFolder(name)
        }
    }
    // The menu with its counts: each folder row carries its count of accounts as the badge of NavigationDrawerItem
    // (GlassMenu's SelectableDropdownMenuItem `count`). With Main alone the folder rows would repeat All, so they come
    // with the first folder the reader makes.
    // https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/SearchBar.kt#AppBarWithSearch
    // not ported: feedPill (a pill with a banner probe; the capsule here serves nobody followed too)
    // not ported: AppBarWithSearch (the app's AppBarWithSearch in ui/component/FeedBanner.kt is its port, used as it is)
    val followed by koinInject<AccountStore>().accounts.collectAsState()
    val feedMenu: @Composable () -> Unit = {
        GlassMenu(expanded = feedsOpen, onDismissRequest = { feedsOpen = false }, contentOffset = CapsuleMenuOffset) {
            // No Clear row: a feed is chosen, not cleared.
            DropdownMenuGroup(index = 0, count = 2) {
                SelectableDropdownMenuItem(selected = state.folder == null, onClick = { showFeed(null) }, text = "All", count = followed.size)
                if (state.folders.size > 1) {
                    state.folders.forEach { name ->
                        SelectableDropdownMenuItem(
                            selected = state.folder == name,
                            onClick = { showFeed(name) },
                            text = name,
                            count = followed.count { it.folder == name }
                        )
                    }
                }
            }
            DropdownMenuGroup(index = 1, count = 2) {
                listOf(LIKED, ARCHIVED).forEach { name ->
                    SelectableDropdownMenuItem(selected = state.folder == name, onClick = { showFeed(name) }, text = name)
                }
            }
        }
    }
    // The capsule's navigation icon: the screen keeps its heading for TalkBack, this wrapper is a heading named "Home", and
    // the switch inside it stays its own node, a button that says the feed and whether its menu is open.
    val feedSwitch: @Composable () -> Unit = {
        Box(Modifier.semantics { heading(); contentDescription = "Home" }) {
            Box {
                FolderSwitch(
                    icon = YidIcons.Folder,
                    trailingIcon = YidIcons.ExpandMore,
                    label = state.folder ?: "All",
                    expanded = feedsOpen,
                    onClick = { feedsOpen = true }
                )
                feedMenu()
            }
        }
    }
    // The alert pops in and out (scale and fade) as the failures come and go, the room it takes opening and closing with it
    // so the capsule's hint does not jump.
    // https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/samples/src/main/java/androidx/compose/animation/samples/AnimatedVisibilitySamples.kt#AnimatedVisibilityWithBooleanVisibleParamNoReceiver
    // not ported: the sample's Box, its fixed size, background and the toggle button around the visible flag
    // not ported: AnimatedVisibilityWithBooleanVisibleParamNoReceiver (the sample's function; its AnimatedVisibility is used here, inline)
    val alert: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = state.errors.isNotEmpty(),
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.6f) + expandHorizontally(tween(200)),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.6f) + shrinkHorizontally(tween(200))
        ) {
            BannerAction(
                icon = YidIcons.Warning,
                label = "What went wrong",
                onClick = { showFailures = true },
                container = MaterialTheme.colorScheme.errorContainer,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    // One glass capsule, nothing under it: the folder switch, "Search saved posts" (hidden while nobody is followed: an
    // empty input field, as the draft draws it), the alert on a failure only. It is an item of the list, so it scrolls away
    // with the posts and is back at the top.
    val banner: @Composable () -> Unit = {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            AppBarWithSearch(
                inputField = {
                    if (state.followedCount > 0) {
                        SearchCapsuleHint(YidIcons.Search, listOf("Search saved posts"), onOpenSearch)
                    }
                },
                navigationIcon = feedSwitch,
                actions = { alert() }
            )
        }
    }

    // A post is read once it has gone past the top of the screen. Everything
    // before the first visible row has, by definition, and one pass of the
    // list costs one write rather than one per card.
    val marks: ReadMarks = koinInject()
    val readIds by marks.read.collectAsState()
    val marksReady by marks.readyState.collectAsState()

    // No top bar at all. A bar that folds away and comes back still owns a
    // band of the screen the whole time it is on it, which on a phone is
    // several lines of a post. The title and its two actions are the first
    // zone of the list instead: they scroll away with everything else, one
    // flick or the back to top button brings them back, and the reading area
    // is the whole window.
    val pull = rememberPullToRefreshState()
    // In glass, the list is recorded as it scrolls, for the controls that
    // float over it (the refresh disc, the folder and back to top buttons)
    // to bend it.
    val look = LocalGlass.current
    val listBackdrop = rememberGlassBackdrop()
    // The page's ground is painted under the whole app, see MainActivity.
    Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) { padding ->
        CompositionLocalProvider(LocalGlassBackdrop provides listBackdrop.takeIf { look != null }) {
        // The check pill lives outside the when, so it is on screen whether
        // Home is empty, failed or full, and whatever the scroll position.
        Box(Modifier.fillMaxSize()) {
        when {
            // Nobody followed: the capsule with the folder switch "All" and no search hint, then the empty zone weighted over
            // what is left, so it sits in the middle of the rest and not against the capsule.
            state.followedCount == 0 -> Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = LocalReadableInset.current)
            ) {
                banner()
                EmptyState(
                    title = "Nothing followed yet",
                    message = "Find a few accounts and they will all appear here in one stream.",
                    actionLabel = "Open Accounts",
                    onAction = onOpenAccounts,
                    modifier = Modifier.weight(1f)
                )
            }

            // Pull works here too, so a failed refresh at launch does not
            // leave Home stuck until a restart.
            state.isEmpty && state.errors.isNotEmpty() -> PullRevealBox(
                isRefreshing = state.loading,
                onRefresh = {
                    askedRefresh()
                    viewModel.refresh()
                },
                modifier = Modifier.fillMaxSize(),
                state = pull
            ) {
                // A list, because the pull gesture needs something scrollable.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = LocalDockPadding.current)
                ) {
                    // The header stays, so a folder whose accounts all failed
                    // is not a dead end: the reader can still switch away.
                    item(key = "home-header") { banner() }
                    item(key = "nothing") {
                        EmptyState(
                            title = "Nothing could be loaded",
                            message = "None of your accounts could be loaded.",
                            actionLabel = "Try again",
                            onAction = { viewModel.refresh() },
                            modifier = Modifier.fillParentMaxHeight(0.8f)
                        )
                    }
                }
            }

            // The pull is Yiḍ's own mark at 72 dp alone in the layer, the list in place: no disc, no count, no ring, no
            // "Updated". The kit's PullRevealBox is the port of IndicatorBox.
            // https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/pulltorefresh/PullToRefresh.kt#IndicatorBox
            // not ported: IndicatorBox's clipRect, shape, containerColor, background, shadowElevation and SpinnerContainerSize
            // (the mark has none, see PullRevealBox)
            else -> PullRevealBox(
                isRefreshing = state.loading,
                onRefresh = {
                    askedRefresh()
                    viewModel.refresh()
                },
                modifier = Modifier.fillMaxSize(),
                state = pull
            ) {
                // Prefetch a page before the reader actually hits the bottom,
                // so scrolling stays continuous instead of stalling.
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        last >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
                    }
                }
                LaunchedEffect(shouldLoadMore, state.canLoadMore, state.pagingFailed) {
                    if (shouldLoadMore) viewModel.loadMore()
                }

                val inline = rememberInlineTarget(
                    listState = listState,
                    posts = state.posts,
                    keyOf = { it.id },
                    paused = viewing != null
                )
                CompositionLocalProvider(LocalInlinePlaying provides inline) {
                // The Home zone is the one row drawn above the posts, so an
                // index in the list turns back into an index in the posts by
                // subtracting it.
                val headerCount = 1

                LaunchedEffect(listState, state.posts, headerCount) {
                    snapshotFlow { listState.firstVisibleItemIndex }
                        .collect { first ->
                            val past = first - headerCount
                            if (past > 0) {
                                marks.markRead(state.posts.take(past).map { it.id })
                            }
                        }
                }
                LazyColumn(
                    state = listState,
                    // Horizontal padding rather than a narrower list, so a
                    // drag in the margins of a wide window scrolls too.
                    // The list starts under the status bar and scrolls beneath it.
                    contentPadding = PaddingValues(
                        start = LocalReadableInset.current,
                        end = LocalReadableInset.current,
                        top = padding.calculateTopPadding(),
                        bottom = LocalDockPadding.current
                    ),
                    modifier = Modifier.fillMaxSize().then(if (look != null) Modifier.glassSource(listBackdrop, look) else Modifier)
                ) {
                    item(key = "home-header") { banner() }

                    if (state.posts.isEmpty() && !state.loading && state.folder != null) {
                        item(key = "folder-empty") {
                            EmptyState(
                                title = "Nothing in ${state.folder} yet",
                                message = "File accounts here from Accounts, or show every account.",
                                actionLabel = "Show every account",
                                onAction = { viewModel.showFolder(null) },
                                modifier = Modifier.fillParentMaxHeight(0.7f)
                            )
                        }
                    }

                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            // Unknown until the file is read, and unknown
                            // means read: a border flashed on every card for
                            // one frame at launch would be worse than none.
                            unread = marksReady && post.id !in readIds,
                            onClick = { onOpenPost(post) },
                            onOpenLink = { uriHandler.openUri(it) },
                            onDownload = { downloader.download(it, post.authorHandle, post.permalink) },
                            showStats = settings.showCounts,
                            onOpenMedia = { index -> viewing = post to index },
                            // Posts arrive and merge while the list is on
                            // screen. Animating placement means a card slides
                            // into its row instead of teleporting.
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "footer") {
                        TimelineFooter(state = state, onLoadMore = { viewModel.loadMore(manual = true) })
                    }
                }
                }

                // The way back to the top shows only once it is a real chore, bottom right above the dock, at the
                // readable column's edge. The folder switch lives in the capsule now, the button stands alone.
                val showBackToTop by remember {
                    derivedStateOf { listState.firstVisibleItemIndex >= BACK_TO_TOP_AFTER }
                }
                ScrollUpButton(
                    visible = showBackToTop,
                    icon = YidIcons.ArrowUp,
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp + LocalReadableInset.current, bottom = 12.dp + LocalDockPadding.current)
                )
            }
        }
        }
        }
    }
}

@Composable
private fun TimelineFooter(state: TimelineUiState, onLoadMore: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.loadingMore -> LoadingMark(size = 28.dp)
            state.pagingFailed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Couldn't load older posts. Bluesky is busy, " +
                        "which usually clears in a minute.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                BoldButton(onClick = onLoadMore) { Text("Try again") }
            }
            state.canLoadMore -> BoldButton(onClick = onLoadMore) { Text("Load older posts") }
            else -> Text(
                "No older posts available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 5

/** Posts scrolled past before the way back becomes worth a button. */
private const val BACK_TO_TOP_AFTER = 5

@Composable
private fun EmptyState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A zone centred in the space, like every empty screen, so an empty
    // Home still carries the wallpaper's colour.
    EmptyZone(
        title = title,
        message = message,
        actionLabel = actionLabel,
        onAction = onAction,
        modifier = modifier.fillMaxSize()
    )
}

private const val LIKED = "Liked"
private const val ARCHIVED = "Archived"

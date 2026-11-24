package com.yid.app.feature.feed

import com.yid.app.ui.component.ZoneAlertDialog
import com.yid.app.ui.component.QuietButton
import androidx.compose.ui.graphics.Color
import com.yid.app.ui.component.RejectOnFailure
import com.yid.app.ui.component.rememberHaptics
import com.yid.app.ui.component.LoadingMark
import com.yid.app.ui.component.PullRevealBox
import com.yid.app.ui.component.BarTitle
import com.yid.app.ui.component.navigationBarBottom
import com.yid.app.ui.component.BoldButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.core.media.MediaDownloader
import com.yid.app.data.settings.SettingsStore
import com.yid.app.core.model.MediaItem
import com.yid.app.core.model.MediaType
import com.yid.app.core.model.Post
import com.yid.app.feature.media.MediaViewer
import com.yid.app.ui.component.FloatingTopBar
import com.yid.app.ui.component.ErrorPanel
import com.yid.app.ui.component.LocalDockPadding
import com.yid.app.ui.component.LocalInlinePlaying
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.component.rememberInlineTarget
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * One account: Bluesky's compact profile header as the list's first item, then its posts.
 *
 * The header is always there, even before anything loaded, so you can follow an account whose feed is momentarily
 * unreachable. While it is on screen no bar is drawn (the system gesture goes back, the pull refreshes); once it has
 * left, the floating capsule comes in with the name as its title.
 * Ports:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/feed/FeedScreen.kt#FeedScreen
 * not ported: kind and AccountKind (LinkedIn's person and company pages: a Bluesky account is one kind)
 * not ported: PullToRefreshBox and LinkedOut's own PullIndicator (Yiḍ calls the kit's PullRevealBox, requests.md point 3)
 * not ported: LinkedOut's glass backdrop under the list and ScrollUpButton (Yiḍ's profile has neither today)
 * not ported: FeedFooter and its AnimatedContent, animateItem of the rows and "LinkedIn shows no posts" (requests.md point 18, the animations)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    handle: String,
    onBack: () -> Unit,
    onOpenPost: (Post) -> Unit,
    viewModel: FeedViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptics = rememberHaptics()
    var confirmUnfollow by remember { mutableStateOf(false) }
    if (confirmUnfollow) {
        ZoneAlertDialog(
            onDismissRequest = { confirmUnfollow = false },
            title = { Text("Unfollow?") },
            confirmButton = {
                QuietButton(onClick = {
                    confirmUnfollow = false
                    haptics.done()
                    viewModel.toggleFollow()
                }) { Text("Unfollow") }
            },
            dismissButton = { QuietButton(onClick = { confirmUnfollow = false }) { Text("Cancel") } }
        )
    }
    RejectOnFailure(state.error)
    val followed by viewModel.followed.collectAsState()
    val isFollowing = followed.any { it.handle.equals(handle, ignoreCase = true) }
    val uriHandler = LocalUriHandler.current
    val downloader: MediaDownloader = koinInject()
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settings.collectAsState()
    val listState = rememberLazyListState()
    // Post id, attachments, start index. The id is what lets the viewer find
    // a saved copy, and it is empty for the avatar and the banner, which belong to no post.
    var viewing by remember { mutableStateOf<Triple<String, List<MediaItem>, Int>?>(null) }

    val feed = state.feed
    val name = feed?.displayName?.takeIf { it.isNotBlank() && it != handle }
    // Declared before the scroll behaviour, which reads it.
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    viewing?.let { (postId, media, index) ->
        MediaViewer(
            postId = postId,
            media = media,
            startIndex = index,
            // The viewer holds the post's id only (empty for the avatar and the banner, which
            // belong to no post): its address is looked up in the feed.
            onDownload = {
                downloader.download(it, handle, feed?.posts?.firstOrNull { post -> post.id == postId }?.permalink)
            },
            onDismiss = { viewing = null }
        )
    }

    LaunchedEffect(handle) { viewModel.load(handle) }

    val shown = feed?.posts.orEmpty()
    val canMore = state.canLoadMore
    val pagingFailed = state.pagingFailed
    val loadingMore = state.loadingMore

    val shouldLoadMore by remember(shown.size) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            shown.isNotEmpty() && last >= shown.size - 5
        }
    }
    LaunchedEffect(shouldLoadMore, canMore, pagingFailed) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    // Material's behaviour consumes every upward drag until its bar exists, so with no bar drawn the list never
    // scrolled and the bar never came. canScroll lets it handle scroll only once the bar is on screen.
    // https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt#enterAlwaysScrollBehavior
    // https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt#EnterAlwaysScrollBehavior
    // not ported: enterAlwaysScrollBehavior (Material's own function is called with canScroll, not copied)
    // not ported: onPreScroll, onPostScroll, onPostFling (the behaviour's connection: Material's own runs, FeedScrollBehaviorTest drives it)
    // not ported: flingAnimationSpec, snapAnimationSpec, isScrollingContentAtStart (the app keeps Material's defaults)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { headerGone })

    Scaffold(
        // The page's ground is painted under the whole app, see MainActivity.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // The header starts at the window's top, its banner under the status bar: the list takes the insets itself.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (headerGone) {
                FloatingTopBar(
                    scrollBehavior = scrollBehavior,
                    title = { BarTitle(name ?: "@$handle") }
                )
            }
        }
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            val inline = rememberInlineTarget(
                listState = listState,
                posts = shown,
                keyOf = { it.id },
                paused = viewing != null
            )
            CompositionLocalProvider(LocalInlinePlaying provides inline) {
                // The pull is Yiḍ's own mark at 72 dp alone in the layer, the list in place (the kit's PullRevealBox).
                // While the first load runs (no feed yet) the page's load mark shows alone, under the header.
                val pull = rememberPullToRefreshState()
                val pulling = state.loading && feed != null
                PullRevealBox(
                    isRefreshing = pulling,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                    state = pull
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        // The header starts at the window's top, so the top is not padded. The readable inset is on
                        // the rows, not the list, so a drag in the margins of a wide window scrolls too and the
                        // banner stays edge to edge.
                        contentPadding = PaddingValues(bottom = navigationBarBottom() + LocalDockPadding.current)
                    ) {
                        item(key = "header") {
                            ProfileHeader(
                                handle = handle,
                                name = name,
                                feed = feed,
                                isFollowing = isFollowing,
                                onToggleFollow = {
                                    // Following answers at once; unfollowing asks first, a stray
                                    // tap on "Following" must not drop an account.
                                    if (isFollowing) {
                                        confirmUnfollow = true
                                    } else {
                                        haptics.done()
                                        viewModel.toggleFollow()
                                    }
                                },
                                // The profile's picture addresses are the originals (BskyParser keeps avatar and banner whole).
                                onOpenAvatar = { url ->
                                    viewing = Triple("", listOf(MediaItem(previewUrl = url, downloadUrl = url, type = MediaType.PHOTO)), 0)
                                },
                                // Shell.tsx onPressBanner opens the lightbox: the viewer gets the banner.
                                // https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/Shell.tsx#onPressBanner
                                // not ported: the lightbox's own moderation and thumbRect
                                onOpenBanner = { url ->
                                    viewing = Triple("", listOf(MediaItem(previewUrl = url, downloadUrl = url, type = MediaType.PHOTO)), 0)
                                }
                            )
                        }

                        // The profile's load mark for the first load, in the page under the header; a refresh of a
                        // profile already shown has the pull's own mark.
                        if (state.loading && feed == null) {
                            item(key = "loading") {
                                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    LoadingMark(size = 72.dp)
                                }
                            }
                        }

                        state.error?.let {
                            item(key = "error") {
                                ErrorPanel(
                                    modifier = Modifier.padding(horizontal = LocalReadableInset.current).padding(16.dp),
                                    error = it,
                                    onRetry = viewModel::refresh
                                )
                            }
                        }

                        if (shown.isEmpty()) {
                            return@LazyColumn
                        }

                        items(shown, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onClick = { onOpenPost(post) },
                                onOpenLink = { uriHandler.openUri(it) },
                                onDownload = { downloader.download(it, post.authorHandle, post.permalink) },
                                showStats = settings.showCounts,
                                onOpenMedia = { index -> viewing = Triple(post.id, post.media, index) },
                                modifier = Modifier.padding(horizontal = LocalReadableInset.current).animateItem()
                            )
                        }

                        item(key = "footer") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = LocalReadableInset.current).padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    loadingMore -> LoadingMark(size = 28.dp)
                                    canMore -> BoldButton(onClick = { viewModel.loadMore(manual = true) }) {
                                        Text(if (pagingFailed) "Try again" else "Load older posts")
                                    }
                                    else -> Text(
                                        "No older posts available.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

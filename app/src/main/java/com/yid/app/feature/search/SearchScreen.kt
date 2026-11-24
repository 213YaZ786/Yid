package com.yid.app.feature.search

import com.yid.app.ui.component.GlassSearchField
import com.yid.app.feature.BarAction
import com.yid.app.ui.component.BarTitle
import com.yid.app.ui.component.FloatingTopBar
import com.yid.app.ui.component.navigationBarBottom
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.derivedStateOf
import com.yid.app.ui.component.plus
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yid.app.core.media.MediaDownloader
import com.yid.app.core.model.Post
import com.yid.app.data.settings.SettingsStore
import com.yid.app.feature.media.MediaViewer
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.icon.YidIcons
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Search through every post saved on this phone, instantly and offline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenPost: (Post) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val typed by viewModel.typed.collectAsState()
    val uriHandler = LocalUriHandler.current
    val focus = LocalFocusManager.current
    val downloader: MediaDownloader = koinInject()
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settings.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var viewing by remember { mutableStateOf<Pair<Post, Int>?>(null) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    viewing?.let { (post, index) ->
        MediaViewer(
            postId = post.id,
            media = post.media,
            startIndex = index,
            onDownload = { downloader.download(it, post.authorHandle, post.permalink) },
            onDismiss = { viewing = null }
        )
    }

    // Ported from LinkedOut's SearchScreen: the bar folds away going down and returns going up, the list full screen.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/search/SearchScreen.kt#SearchScreen
    // The title shows once the field, the list's first row, has scrolled away (the validated picture, as Yiḍ's FeedScreen headerGone).
    // not ported: AnimatedVisibility of the Clear icon, Crossfade of the status line, animateItem of that line (point 18, the animations)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // The page's ground is painted under the whole app, see MainActivity.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        // The status and navigation bars are the list's own, in its content padding.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FloatingTopBar(
                scrollBehavior = scrollBehavior,
                title = { if (headerGone) BarTitle("Search") },
                navigationIcon = { BarAction(YidIcons.ArrowBack, "Back", onBack) }
            )
        }
    ) { padding ->
        val hint = when {
            !state.loaded -> null
            state.savedCount == 0 -> "Nothing saved yet. Posts you read are kept here and can be searched offline."
            typed.isBlank() -> "Search through ${state.savedCount} posts saved on this phone, offline. " +
                "Words can be in any order."
            state.results.isEmpty() -> "No saved post matches."
            else -> null
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Horizontal padding rather than a narrower list, so a drag in
            // the margins of a wide window scrolls too.
            contentPadding = PaddingValues(
                start = LocalReadableInset.current,
                end = LocalReadableInset.current,
                bottom = navigationBarBottom()
            ).plus(padding)
        ) {
            // The list's first row, under the bar, as LinkedOut's SearchScreen sets its field.
            // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/search/SearchScreen.kt#SearchScreen
            item(key = "field") {
                GlassSearchField(
                    value = typed,
                    onValueChange = viewModel::setQuery,
                    placeholder = "Search saved posts",
                    leadingIcon = { Icon(YidIcons.Search, contentDescription = null) },
                    trailingIcon = {
                        if (typed.isNotEmpty()) {
                            // A tap target of 48 dp in the field's glass, without a Material icon button.
                            Icon(
                                YidIcons.Close,
                                contentDescription = "Clear",
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) { viewModel.setQuery("") }
                                    .padding(12.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            if (hint != null) {
                item(key = "hint") {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            } else if (state.results.isNotEmpty()) {
                item(key = "count") {
                    Text(
                        if (state.results.size == 1) "1 post" else "${state.results.size} posts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            items(state.results, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onClick = {
                        focus.clearFocus()
                        onOpenPost(post)
                    },
                    onOpenLink = { uriHandler.openUri(it) },
                    onDownload = { downloader.download(it, post.authorHandle, post.permalink) },
                    showStats = settings.showCounts,
                    onOpenMedia = { index -> viewing = post to index },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

package com.yid.app.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.model.Post
import com.yid.app.core.model.PostStats
import com.yid.app.di.appModule
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.BarTitle
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.FloatingTopBar
import com.yid.app.ui.component.GlassSearchField
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.component.ZoneSurface
import com.yid.app.ui.component.navigationBarBottom
import com.yid.app.ui.component.plus
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.glass.groundHere
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DRAFT for the user's yes (requests.md point 15, MTGA's point 31, LinkedOut's points 17 and 26), nothing of app/src/main
 * changes: the content scrolls on the whole screen with no fade at all (no underFloatingTopBar), and the top capsule
 * folds away while scrolling down and comes back on the first upward scroll: Material's enterAlwaysScrollBehavior
 * (EnterAlwaysScrollBehavior, its nestedScrollConnection and heightOffset), which FloatingTopBar already supports through
 * its scrollBehavior:
 * https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt#enterAlwaysScrollBehavior
 * The post page is scrolled four rows down, the frame is [PostDraftScreen]: scroll_full_screen_draft, the capsule fully
 * folded (rows up to the window's top, under the status bar); scroll_full_screen_draft_back, the capsule coming back (half
 * the offset, a scroll up). Search and the Activity log take the floating capsule too (back left, the title only once their
 * header has scrolled away): search_capsule_draft and activity_log_capsule_draft are the pages scrolled, _top the pages at
 * rest, _today their screens today (Search's TopAppBar with the field as its title, the Activity log's TopAppBar over its
 * two buttons: no ScreenBanner is left in Yiḍ's screens since points 10 and 11).
 * Ported from LinkedOut's draft (validated), itself MTGA's ScrollFullScreenDraft (validated):
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/ScrollFullScreenDraftScreenshotTest.kt#ScrollFullScreenDraftScreenshotTest
 * with its screens redrawn from Yiḍ's own, faithful copies with fixed data (PostDetailScreen's ConversationView is bound
 * to its view model):
 * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/post/PostDetailScreen.kt#ConversationView
 * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/search/SearchScreen.kt#SearchScreen
 * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/debug/DebugLogScreen.kt#DebugLogScreen
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/debug/DebugLogScreen.kt#DebugLogScreen
 * Settings is not drawn: its header (FloatingFrame's top and large title) has no scrollBehavior, so it cannot fold the
 * same way without a new mechanism.
 * not ported: EnterAlwaysScrollBehavior's flingAnimationSpec and snapAnimationSpec (a still picture, no fling)
 * not ported: LinkedOut's ConnectionDraftScreen host cards, HostCard, DraftHost and draftHosts (LinkedOut's Connection
 * screen and its HostThrottle: Yiḍ has no such screen, so the rows are its own post page, its replies and its log
 * entries, repeated so the list is long enough to scroll); MTGA's checking state, FeatherRing, OverallBanner, InstanceCard,
 * StatusDot, DraftServer and draftServers (MTGA's server list); the bar's Add and Re-check actions; DebugLogScreen's
 * snackbar, clipboard, exporter, the last page line and animateItem (nothing is tapped)
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class ScrollFullScreenDraftScreenshotTest : ScreenshotBase() {

    // PostCard reads the post marks through Koin.
    @Before
    fun startKoinOnce() {
        startKoin {
            androidContext(compose.activity.applicationContext)
            modules(appModule)
        }
    }

    @After
    fun stopKoinAgain() = stopKoin()

    // The bar's height is measured after the first frame, and the offset follows it one frame later: frames are given.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/ScrollFullScreenDraftScreenshotTest.kt#settle
    private fun settle() {
        repeat(3) { compose.mainClock.advanceTimeByFrame() }
        compose.waitForIdle()
    }

    // The cards write the age against the clock, so the data is dated from the fixed instant of ScreenshotBase.
    private val now = FIXED_NOW

    private val posts = listOf(
        draftPost("ada.bsky.social", "Ada Lindqvist", "3kdraft0001", 2, "Finished the first draft of the trail guide. Photos of the ridge come next week."),
        draftPost("bram.example.com", "Bram Okafor", "3kdraft0002", 26, "Three things we learned running a small server for a hundred friends."),
        draftPost("cleo.bsky.social", "Cleo Marsh", "3kdraft0003", 3 * 24, "A good week for the people building in the open. Thank you to every maintainer."),
        draftPost("dara.example.org", "Dara Whitfield", "3kdraft0004", 4 * 24, "Notes from the small-press fair: the zines that sold out first were the ones with a map."),
        draftPost("eli.bsky.social", "Eli Navarro", "3kdraft0005", 5 * 24, "The ridge walk is open again. Bring water, the spring is dry."),
        draftPost("fay.bsky.social", "Fay Okonkwo", "3kdraft0006", 6 * 24, "Reading the old trail logs, someone wrote the same warning in 1994."),
        draftPost("gus.example.net", "Gus Haraldsen", "3kdraft0007", 7 * 24, "Mapped the whole valley by foot this month. The upload comes once the batteries recover.")
    )

    private fun draftPost(handle: String, name: String, rkey: String, hoursAgo: Long, text: String) = Post(
        id = "at://did:plc:draft$handle/app.bsky.feed.post/$rkey",
        authorHandle = handle,
        authorName = name,
        text = text,
        publishedAtMillis = now - hoursAgo * 60 * 60 * 1000,
        permalink = "https://bsky.app/profile/$handle/post/$rkey",
        stats = PostStats(replies = 4, reposts = 9, quotes = 1, likes = 210)
    )

    // A log entry as the Activity log draws one: the time, the kind and its outcome, the url, then the numbers.
    private class DraftEntry(val atMillis: Long, val kind: String, val outcome: String, val url: String, val status: Int, val millis: Long, val bytes: Int)

    private val entries = List(14) { index ->
        DraftEntry(
            atMillis = now - index * 37_000L,
            kind = if (index % 3 == 0) "profile" else "feed",
            outcome = if (index == 2) "rate limited" else "ok",
            url = "https://public.api.bsky.app/xrpc/app.bsky.feed.getAuthorFeed?actor=ada.bsky.social&limit=30",
            status = if (index == 2) 429 else 200,
            millis = 180L + index * 23,
            bytes = 41_000 + index * 1_300
        )
    }

    @Test
    fun folded() = capture("scroll_full_screen_draft", interact = ::settle) { PostDraftScreen(firstItem = 4, foldedBy = 1f) }

    @Test
    fun back() = capture("scroll_full_screen_draft_back", interact = ::settle) { PostDraftScreen(firstItem = 4, foldedBy = 0.5f) }

    // Search and the Activity log under the capsule: the page scrolled (the header gone, the title in), then at rest.
    @Test
    fun searchScrolled() = capture("search_capsule_draft", interact = ::settle) { SearchDraftScreen(scrolled = true) }

    @Test
    fun searchTop() = capture("search_capsule_draft_top", interact = ::settle) { SearchDraftScreen(scrolled = false) }

    @Test
    fun activityScrolled() = capture("activity_log_capsule_draft", interact = ::settle) { ActivityDraftScreen(scrolled = true) }

    @Test
    fun activityTop() = capture("activity_log_capsule_draft_top", interact = ::settle) { ActivityDraftScreen(scrolled = false) }

    // Today's two screens, for the comparison.
    @Test
    fun searchToday() = capture("search_capsule_today", interact = ::settle) { SearchToday() }

    @Test
    fun activityToday() = capture("activity_log_capsule_today", interact = ::settle) { ActivityToday() }

    // Edge: the capsule fully folded, at 200 % text and on a 360 dp phone.
    @Test
    fun foldedEdge() = captureEdge("scroll_full_screen_draft", interact = ::settle) { PostDraftScreen(firstItem = 4, foldedBy = 1f) }

    @Test
    fun searchEdge() = captureEdge("search_capsule_draft", interact = ::settle) { SearchDraftScreen(scrolled = true) }

    @Test
    fun activityEdge() = captureEdge("activity_log_capsule_draft", interact = ::settle) { ActivityDraftScreen(scrolled = true) }

    /**
     * PostDetailScreen's ConversationView with fixed data: the post, then its replies. [firstItem] scrolls the list that
     * many rows down; [foldedBy] is how far the bar has folded away, as enterAlwaysScrollBehavior's heightOffset runs from 0
     * (in place) to heightOffsetLimit (fully folded): 1 is fully folded, 0 in place. Material's own state did not settle in
     * Robolectric's frames in MTGA's draft (the bar stayed out of the picture at half), so the offset is set on the bar's
     * layer, as there.
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/ScrollFullScreenDraftScreenshotTest.kt#ConnectionDraftScreen
     */
    @Composable
    private fun PostDraftScreen(firstItem: Int = 0, foldedBy: Float? = null) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                FloatingTopBar(
                    modifier = Modifier.graphicsLayer { translationY = -(foldedBy ?: 0f) * size.height },
                    title = { BarTitle("Post") },
                    navigationIcon = { BarAction(YidIcons.ArrowBack, "Back") },
                    actions = { BarAction(YidIcons.Share, "Share") }
                )
            }
        ) { padding ->
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = firstItem)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = LocalReadableInset.current,
                    end = LocalReadableInset.current,
                    bottom = navigationBarBottom()
                ).plus(padding)
            ) {
                item(key = "main") { PostCard(post = posts[0], onClick = {}, onOpenLink = {}, onDownload = {}) }
                item(key = "replieslabel") { SectionLabel("Replies") }
                items(posts.drop(1), key = { it.id }) { PostCard(post = it, onClick = {}, onOpenLink = {}, onDownload = {}) }
            }
        }
    }

    // PostDetailScreen's SectionLabel.
    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/post/PostDetailScreen.kt#SectionLabel
    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
        )
    }

    // SearchScreen under the capsule: the field is the list's first row, the count the second, then the results. Scrolled,
    // the field has left and the capsule shows the title.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/search/SearchScreen.kt#SearchScreen
    @Composable
    private fun SearchDraftScreen(scrolled: Boolean) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                FloatingTopBar(
                    title = { if (scrolled) BarTitle("Search") },
                    navigationIcon = { BarAction(YidIcons.ArrowBack, "Back") }
                )
            }
        ) { padding ->
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (scrolled) 2 else 0)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = LocalReadableInset.current,
                    end = LocalReadableInset.current,
                    bottom = navigationBarBottom()
                ).plus(padding)
            ) {
                item(key = "field") {
                    GlassSearchField(
                        value = "trail",
                        onValueChange = {},
                        placeholder = "Search saved posts",
                        leadingIcon = { Icon(YidIcons.Search, contentDescription = null) },
                        trailingIcon = { Icon(YidIcons.Close, contentDescription = "Clear", modifier = Modifier.padding(12.dp)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions.Default,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item(key = "count") { CountLine("${posts.size} posts") }
                items(posts, key = { it.id }) { PostCard(post = it, onClick = {}, onOpenLink = {}, onDownload = {}) }
            }
        }
    }

    // DebugLogScreen under the capsule: Back left, Clear right, the two buttons the list's first row, then the entries.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/main/java/com/linkedout/app/feature/debug/DebugLogScreen.kt#DebugLogScreen
    @Composable
    private fun ActivityDraftScreen(scrolled: Boolean) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                FloatingTopBar(
                    title = { if (scrolled) BarTitle("Activity log") },
                    navigationIcon = { BarAction(YidIcons.ArrowBack, "Back") },
                    actions = { BarAction(YidIcons.Delete, "Clear") }
                )
            }
        ) { padding ->
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (scrolled) 2 else 0)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = LocalReadableInset.current,
                    end = LocalReadableInset.current,
                    bottom = 12.dp + navigationBarBottom()
                ).plus(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "actions") { LogButtons(Modifier.padding(horizontal = 8.dp)) }
                items(entries, key = { it.atMillis }) { EntryCard(it, Modifier.padding(horizontal = 12.dp)) }
            }
        }
    }

    // Today's Search (SearchScreen.kt): a TopAppBar on the page's ground, Back, the field as its title; the list under it.
    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/search/SearchScreen.kt#SearchScreen
    @Composable
    private fun SearchToday() {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    modifier = Modifier.groundHere(LocalGlass.current, MaterialTheme.colorScheme.background),
                    navigationIcon = { BarAction(YidIcons.ArrowBack, "Back") },
                    title = {
                        GlassSearchField(
                            value = "trail",
                            onValueChange = {},
                            placeholder = "Search saved posts",
                            trailingIcon = { Icon(YidIcons.Close, contentDescription = "Clear", modifier = Modifier.padding(12.dp)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions.Default,
                            modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                        )
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = LocalReadableInset.current, end = LocalReadableInset.current).plus(padding)
            ) {
                item(key = "count") { CountLine("${posts.size} posts") }
                items(posts, key = { it.id }) { PostCard(post = it, onClick = {}, onOpenLink = {}, onDownload = {}) }
            }
        }
    }

    // Today's Activity log (DebugLogScreen.kt): a TopAppBar with the title in the accent, Back, Clear; the two buttons, the entries.
    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/debug/DebugLogScreen.kt#DebugLogScreen
    @Composable
    private fun ActivityToday() {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.groundHere(LocalGlass.current, MaterialTheme.colorScheme.background).padding(horizontal = LocalReadableInset.current),
                    title = { Text("Activity log") },
                    navigationIcon = { BarAction(YidIcons.ArrowBack, "Back") },
                    actions = { BarAction(YidIcons.Delete, "Clear") }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                LogButtons(Modifier.padding(horizontal = 8.dp + LocalReadableInset.current))
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 12.dp + LocalReadableInset.current,
                        end = 12.dp + LocalReadableInset.current,
                        top = 12.dp,
                        bottom = 12.dp + padding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.atMillis }) { EntryCard(it, Modifier) }
                }
            }
        }
    }

    // SearchScreen's count line: "N posts" at the start, in the variant colour.
    @Composable
    private fun CountLine(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    // DebugLogScreen's two buttons.
    @Composable
    private fun LogButtons(modifier: Modifier) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BoldButton(onClick = {}) { Text("Copy all") }
            BoldButton(onClick = {}) { Text("Save to Downloads") }
        }
    }

    // DebugLogScreen's entry: a zone of the container colour, the stamp, the kind and the outcome, the url, the numbers.
    @Composable
    private fun EntryCard(entry: DraftEntry, modifier: Modifier) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US)
        ZoneSurface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "${stamp.format(Date(entry.atMillis))}  ${entry.kind}  ${entry.outcome}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    entry.url,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "http ${entry.status}, ${entry.millis}ms, ${entry.bytes} bytes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // The bar's round targets: a transparent Surface of 48 dp, what IconButton is (the guard refuses it in app code).
    @Composable
    private fun BarAction(icon: ImageVector, label: String) {
        Surface(onClick = {}, shape = CircleShape, color = Color.Transparent) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            }
        }
    }
}

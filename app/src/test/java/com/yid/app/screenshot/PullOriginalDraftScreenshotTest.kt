package com.yid.app.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.model.Post
import com.yid.app.core.model.PostStats
import com.yid.app.di.appModule
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.AppBarWithSearch
import com.yid.app.ui.component.FolderSwitch
import com.yid.app.ui.component.LoadingMark
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.component.SearchCapsuleHint
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.glass.LocalGlassBackdrop
import com.yid.app.ui.glass.glassFloating
import com.yid.app.ui.glass.glassSource
import com.yid.app.ui.glass.rememberGlassBackdrop
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
import kotlin.math.max

/**
 * DRAFT for the user's yes (requests.md point 3), nothing of app/src/main changes: the pull to refresh of Home with Yiḍ's
 * own mark, the bat and its star, at 72 dp straight in the layer: no bubble, no disc, no glass, no shadow, no ring, no count,
 * no "Updated". The list stays where it is, the mark slides down over it from the top edge and lands just under the capsule,
 * over the first post.
 * pull_original_draft_half: half of the threshold (the mark's alpha has reached 1, the star is half lit).
 * pull_original_draft_threshold: the threshold (the mark's foot lands 4 dp under the capsule, the star at its brightest).
 * pull_original_draft_refreshing: the mark running at the threshold (the star twinkling on its own clock, at rest here).
 * pull_original_draft_today_threshold: the pull as ui/component/PullIndicator.kt draws it now, the 44 dp mark in a 56 dp
 * circular glass disc (the plain surfaceContainerHigh disc with a shadow when there is no glass) sliding down over the list.
 * Home is the capsule of point 10 (the folder switch "All", "Search saved posts") over the first posts, as
 * TopCapsuleDraftScreenshotTest draws it.
 * Ported from LinkedOut's draft of the same point, through its own pull and mark (Yiḍ's bat in place of its pen):
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PullLoadDraftScreenshotTest.kt#PullLoadDraftScreenshotTest
 * The layer is PullRevealBox's PullMarkBox (ui/component/PullIndicator.kt, private there), IndicatorBox of Material 3 without
 * its shape, background and elevation, redrawn here because the state holds the pull by a finger only:
 * https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/pulltorefresh/PullToRefresh.kt#IndicatorBox
 * not ported: LinkedOut's Profile and ProfileCard, profileToday and profileProposed (Yiḍ's profile has no pull yet: its own
 * picture comes with point 16), MarkStandIn (Yiḍ's LoadingMark draws on Robolectric, the real mark is drawn), MTGA's
 * FeatherRing, its counts and the blurred page; the haptic tick of PullIndicator and its snapshotFlow (a picture feels
 * nothing); PullIndicator's animateFloatAsState hold (the refreshing frame holds alpha 1 at once).
 * Robolectric holds the clock still: nothing here moves, each frame is one instant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class PullOriginalDraftScreenshotTest : ScreenshotBase() {

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

    // The cards write the age against the clock, so the data is dated from the fixed instant of ScreenshotBase (a field is
    // built before the rule that sets the clock, so the constant is read, not the clock).
    private val now = FIXED_NOW

    private val posts = listOf(
        Post(
            id = "at://did:plc:draftada/app.bsky.feed.post/3kdraft0001",
            authorHandle = "ada.bsky.social",
            authorName = "Ada Lindqvist",
            text = "Finished the first draft of the trail guide. Photos of the ridge come next week.",
            publishedAtMillis = now - 2 * 60 * 60 * 1000,
            permalink = "https://bsky.app/profile/ada.bsky.social/post/3kdraft0001",
            stats = PostStats(replies = 12, reposts = 41, quotes = 3, likes = 930)
        ),
        Post(
            id = "at://did:plc:draftbram/app.bsky.feed.post/3kdraft0002",
            authorHandle = "bram.example.com",
            authorName = "Bram Okafor",
            text = "Three things we learned running a small server for a hundred friends.",
            publishedAtMillis = now - 26 * 60 * 60 * 1000,
            permalink = "https://bsky.app/profile/bram.example.com/post/3kdraft0002",
            stats = PostStats(replies = 4, reposts = 9, quotes = 1, likes = 210)
        ),
        Post(
            id = "at://did:plc:draftcleo/app.bsky.feed.post/3kdraft0003",
            authorHandle = "cleo.bsky.social",
            authorName = "Cleo Marsh",
            text = "A good week for the people building in the open. Thank you to every maintainer.",
            publishedAtMillis = now - 3 * 24 * 60 * 60 * 1000,
            permalink = "https://bsky.app/profile/cleo.bsky.social/post/3kdraft0003",
            stats = PostStats(replies = 2, reposts = 6, quotes = 0, likes = 145)
        )
    )

    // The pull's instants: the mark at half of the threshold, at the threshold, and refreshing at the threshold.
    @Test
    fun proposedHalf() = capture("pull_original_draft_half") { Screen(fraction = 0.5f, refreshing = false, proposed = true) }

    @Test
    fun proposedThreshold() = capture("pull_original_draft_threshold") { Screen(fraction = 1f, refreshing = false, proposed = true) }

    @Test
    fun proposedRefreshing() = capture("pull_original_draft_refreshing") { Screen(fraction = 1f, refreshing = true, proposed = true) }

    // Today's disc beside it, at the threshold.
    @Test
    fun todayThreshold() = capture("pull_original_draft_today_threshold") { Screen(fraction = 1f, refreshing = false, proposed = false) }

    // Edge: the proposal at the threshold, at 200 % text and on a 360 dp phone.
    @Test
    fun edge() = captureEdge("pull_original_draft_threshold") { Screen(fraction = 1f, refreshing = false, proposed = true) }

    // The page and the indicator over it, placed as PullRevealBox places its mark: top centre, under the status bar.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PullLoadDraftScreenshotTest.kt#Screen
    @Composable
    private fun Screen(fraction: Float, refreshing: Boolean, proposed: Boolean) {
        val look = LocalGlass.current
        val backdrop = rememberGlassBackdrop()
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop.takeIf { look != null }) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().then(if (look != null) Modifier.glassSource(backdrop, look) else Modifier)) {
                    Feed()
                }
                val place = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars)
                if (proposed) {
                    ProposedIndicator(fraction, refreshing, place)
                } else {
                    TodayIndicator(fraction, refreshing, place)
                }
            }
        }
    }

    // Home as the capsule of point 10 draws it, then a few cards, in the list's side inset (the capsule is the first item of
    // the list and the list does not move).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PullLoadDraftScreenshotTest.kt#Feed
    @Composable
    private fun Feed() {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = LocalReadableInset.current)) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AppBarWithSearch(
                    inputField = { SearchCapsuleHint(YidIcons.Search, listOf("Search saved posts"), onClick = {}) },
                    navigationIcon = {
                        FolderSwitch(
                            icon = YidIcons.Folder,
                            trailingIcon = DraftIcons.ExpandMore,
                            label = "All",
                            expanded = false,
                            onClick = {}
                        )
                    }
                )
            }
            posts.forEach { PostCard(post = it, onClick = {}, onOpenLink = {}, onDownload = {}, unread = it === posts.first()) }
        }
    }

    /**
     * PullIndicator's own layer and disc: the mark slides down from the top edge by fraction x threshold, its alpha the
     * pull's fade-in or 1 while refreshing; in glass the disc bends the list under it with no shadow, else a
     * surfaceContainerHigh disc with a 3 dp shadow. 44 dp mark in a 56 dp disc.
     * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PullLoadDraftScreenshotTest.kt#TodayIndicator
     */
    @Composable
    private fun TodayIndicator(fraction: Float, refreshing: Boolean, modifier: Modifier) {
        val threshold = PullToRefreshDefaults.PositionalThreshold
        Box(
            modifier.graphicsLayer {
                translationY = fraction * threshold.toPx() - this.size.height
                alpha = max((fraction * 2f).coerceIn(0f, 1f), if (refreshing) 1f else 0f)
            }
        ) {
            val look = LocalGlass.current
            val backdrop = LocalGlassBackdrop.current
            val glass = look != null && backdrop != null
            Surface(
                shape = CircleShape,
                color = if (glass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = if (glass) 0.dp else 3.dp,
                modifier = if (glass) Modifier.size(56.dp).glassFloating(backdrop, CircleShape, look) else Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LoadingMark(size = 44.dp, running = refreshing, progress = { fraction.coerceIn(0f, 1f) })
                }
            }
        }
    }

    // The proposal: PullMarkBox's layer and instants, no Surface, no glass, no shadow; the mark alone at 72 dp. The mark lands
    // 140 dp down (the banner's 64 dp, the mark's 72 dp and 4 dp of air, PullMaxDistance of ui/component/PullIndicator.kt).
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PullLoadDraftScreenshotTest.kt#ProposedIndicator
    @Composable
    private fun ProposedIndicator(fraction: Float, refreshing: Boolean, modifier: Modifier) {
        val maxDistance: Dp = 140.dp
        Box(
            modifier.size(72.dp).graphicsLayer {
                translationY = fraction * maxDistance.toPx() - this.size.height
                alpha = if (refreshing) 1f else (fraction * 2f).coerceIn(0f, 1f)
            },
            contentAlignment = Alignment.Center
        ) {
            LoadingMark(size = 72.dp, running = refreshing, progress = { fraction.coerceIn(0f, 1f) })
        }
    }
}

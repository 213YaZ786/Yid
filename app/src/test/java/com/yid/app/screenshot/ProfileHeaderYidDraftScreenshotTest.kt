package com.yid.app.screenshot

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yid.app.core.model.Post
import com.yid.app.core.model.PostStats
import com.yid.app.di.appModule
import com.yid.app.navigation.LocalReadableInset
import com.yid.app.ui.component.Avatar
import com.yid.app.ui.component.BoldButton
import com.yid.app.ui.component.FloatingTopBar
import com.yid.app.ui.component.GlassTarget
import com.yid.app.ui.component.LoadingMark
import com.yid.app.ui.component.PostCard
import com.yid.app.ui.component.relativeTime
import com.yid.app.ui.component.statusBarTop
import com.yid.app.ui.glass.LocalGlass
import com.yid.app.ui.glass.glassZone
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
import java.util.Locale

/**
 * DRAFT for the user's yes (requests.md points 16 and 17), nothing of app/src/main changes: Yiḍ's profile takes Bluesky's
 * own compact header (ProfileHeaderStandard on ProfileHeaderShell), as LinkedOut's draft draws it for LinkedIn's fields.
 * The banner 150 dp edge to edge from the window's top (status bar shadow kept; its picture is a STAND-IN, a vertical
 * Brush of primary to tertiary), no Back button (the system gesture) and no Refresh button (a pull does it), the avatar
 * 90 dp in a 2 dp ring of the page's colour at the start over the banner's bottom edge, Follow at the end below the
 * banner (Material 3 Expressive's medium button, 56 dp, round, drawn as a glass zone: filled with the primary and its
 * label in onPrimary, or once followed the same size, glass only, with a check), then at the start the name (titleLarge
 * bold, wrapping, the verified check inline at its end), the handle, the counts on one line with bold numbers (followers,
 * following, posts: Bluesky's ProfileHeaderMetrics), the bio, and Joined with its icon (what Yiḍ reads of getProfile).
 * No website and no location: Bluesky's profile has none.
 * Frames: profile_header_yid_draft_person, _long, _following, _pull (Yiḍ's own mark, the bat and its star, 72 dp with no
 * disc, over the banner, the header unchanged under it), _today (today's header redrawn from FeedScreen, with the bar's
 * Back and Refresh, for the comparison), each on phone and tablet, light and dark, and the edge pictures (200 % text, 360 dp
 * phone) of the person and the long one.
 * Ports (Bluesky's, MIT):
 * https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/ProfileHeaderStandard.tsx#ProfileHeaderStandard
 * https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/ProfileHeaderStandard.tsx#HeaderStandardButtons
 * https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/Shell.tsx#ProfileHeaderShell
 * https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/Metrics.tsx#ProfileHeaderMetrics
 * https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/DisplayName.tsx#ProfileHeaderDisplayName
 * https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/layout.ts#PROFILE_BANNER_HEIGHT
 * and from LinkedOut's draft of the same point (validated):
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/ProfileHeaderLinkedOutDraftScreenshotTest.kt#ProfileHeaderLinkedOutDraftScreenshotTest
 * The verified check is point 17's VerificationCheck (PostMarksYidDraftScreenshotTest.kt), in the primary. Icons, Material
 * Symbols Rounded: person_add, calendar_month
 * https://github.com/google/material-design-icons/blob/master/symbols/android/person_add/materialsymbolsrounded/person_add_24px.xml
 * https://github.com/google/material-design-icons/blob/master/symbols/android/calendar_month/materialsymbolsrounded/calendar_month_24px.xml
 * The pull layer is the local draft's (Yiḍ's real LoadingMark draws on Robolectric), today's header its redraw of FeedScreen:
 * https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PullLoadDraftScreenshotTest.kt#ProposedIndicator
 * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/feed/FeedScreen.kt#ProfileHeader
 * not ported: ProfileHeaderStandard's moderation (moderateProfile, the blur, the labels, the alerts), the block and unblock
 * prompt, the suggested follows, the live status, DebugFieldDisplay, ProfileHeaderLinks, the known followers, GermButton,
 * the RichText with its link and tag facets and `shouldProxyLinks` (the bio is plain text in Yiḍ today, a tap on a tag or a
 * mention is point 21's); HeaderStandardButtons' Edit profile, Invite friends, Unblock, SubscribeProfileButton,
 * MessageProfileButton, ProfileMenu, Follow back and the toasts (a reader has no account: only Follow and Following);
 * Shell's LoadingPlaceholder, the labeler's rect avatar, LabelsOnMe, EditLiveDialog, the lightbox's thumbRect and refs, the
 * back button (hideBackButton: the system gesture), GrowableBanner and GrowableAvatar's pull zoom (Yiḍ pulls its own mark);
 * Metrics' links to followers and follows, `formatCount` and `plural` (fixed strings); Handle.tsx's pronouns and "Follows
 * you" (needs a viewer); LinkedOut's company, headline, location, school and website rows and its MarkStandIn (LinkedOut's
 * pen: Yiḍ's real LoadingMark is drawn)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [36])
class ProfileHeaderYidDraftScreenshotTest : ScreenshotBase() {

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

    // The cards write the age against the clock, so the data is dated from the fixed instant of ScreenshotBase.
    private val now = FIXED_NOW

    private fun posts(content: HeaderContent) = listOf(
        Post(
            id = "at://did:plc:draft/app.bsky.feed.post/3kdraft0101",
            authorHandle = content.handle,
            authorName = content.name,
            text = "Finished the first draft of the trail guide. Photos of the ridge come next week.",
            publishedAtMillis = now - 3 * 60 * 60 * 1000,
            permalink = "https://bsky.app/profile/${content.handle}/post/3kdraft0101",
            stats = PostStats(replies = 12, reposts = 31, quotes = 2, likes = 540)
        ),
        Post(
            id = "at://did:plc:draft/app.bsky.feed.post/3kdraft0102",
            authorHandle = content.handle,
            authorName = content.name,
            text = "Three things we learned from a season of walking the same valley every week.",
            publishedAtMillis = now - 2 * 24 * 60 * 60 * 1000,
            permalink = "https://bsky.app/profile/${content.handle}/post/3kdraft0102",
            stats = PostStats(replies = 4, reposts = 9, quotes = 1, likes = 210)
        )
    )

    // A person at rest: a short bio, the counts, Joined.
    @Test
    fun person() = capture("profile_header_yid_draft_person") { Screen(following = false, content = PersonContent) }

    // Long content: a three line name with its check, a long handle, a long bio.
    @Test
    fun long() = capture("profile_header_yid_draft_long") { Screen(following = false, content = LongContent) }

    // The second state of the button: Following.
    @Test
    fun followingState() = capture("profile_header_yid_draft_following") { Screen(following = true, content = PersonContent) }

    // The profile pulled down: Yiḍ's own mark, 72 dp with no disc, over the banner, the header unchanged under it.
    @Test
    fun pull() = capture("profile_header_yid_draft_pull") {
        Box(Modifier.fillMaxSize()) {
            Screen(following = false, content = PersonContent)
            PullIndicator(fraction = 1f, refreshing = false, modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBarTop()))
        }
    }

    // Today's header, redrawn from FeedScreen, for the comparison.
    @Test
    fun today() = capture("profile_header_yid_draft_today") { Today(content = PersonContent) }

    // Edge: 200 % text and a 360 dp phone.
    @Test
    fun edge() = captureEdge("profile_header_yid_draft_person") { Screen(following = false, content = PersonContent) }

    // The same long content at 200 % text and a 360 dp phone.
    @Test
    fun longEdge() = captureEdge("profile_header_yid_draft_long") { Screen(following = false, content = LongContent) }

    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/ProfileHeaderLinkedOutDraftScreenshotTest.kt#Screen
    @Composable
    private fun Screen(following: Boolean, content: HeaderContent) {
        Column(Modifier.fillMaxSize()) {
            DraftHeader(following, content)
            Column(
                Modifier.padding(horizontal = LocalReadableInset.current),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                posts(content).forEach { PostCard(post = it, onClick = {}, onOpenLink = {}, onDownload = {}) }
            }
        }
    }

    // Today: the floating bar with Back and Refresh over the list, the header as its first item, then the posts.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/ProfileHeaderLinkedOutDraftScreenshotTest.kt#Today
    @Composable
    private fun Today(content: HeaderContent) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(top = statusBarTop() + 64.dp)) {
                ProfileCard(content)
                Column(
                    Modifier.padding(horizontal = LocalReadableInset.current),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    posts(content).forEach { PostCard(post = it, onClick = {}, onOpenLink = {}, onDownload = {}) }
                }
            }
            // FeedScreen's bar: Back, the title only once the header has gone, Refresh.
            FloatingTopBar(
                title = {},
                navigationIcon = { BarAction(YidIcons.ArrowBack, "Back") },
                actions = { BarAction(YidIcons.Refresh, "Refresh") }
            )
        }
    }

    /**
     * FeedScreen.kt's ProfileHeader with fixed data: the banner inset 24 dp from the sides, 3:1, 16 dp corners (a stand-in
     * picture), the 88 dp avatar centred, the name in headlineSmall, the handle, the bio, Joined, the counts, Follow or
     * Following as the app's own BoldButton, the source line.
     * https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/feed/FeedScreen.kt#ProfileHeader
     * not ported: the media policy hold, AsyncImage and the avatar's tap (no network picture on Robolectric)
     */
    @Composable
    private fun ProfileCard(content: HeaderContent) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
            )
            Avatar(url = null, name = content.name, size = 88.dp)
            Text(
                content.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                "@${content.handle}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                content.bio,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 520.dp).padding(top = 4.dp)
            )
            Text(
                content.joined,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                listOf("${compactCount(content.posts)} posts", "${compactCount(content.following)} following", "${compactCount(content.followers)} followers")
                    .joinToString(", "),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
            BoldButton(filled = true, onClick = {}, modifier = Modifier.padding(top = 8.dp)) { Text("Follow") }
            Text(
                "Read via public.api.bsky.app, updated ${relativeTime(now - 12 * 60 * 1000)} ago",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    // The proposal's pull layer: no Surface, no glass, no shadow; the mark alone at 72 dp sliding down from the top edge by
    // fraction x the distance, over the banner.
    // https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/PullLoadDraftScreenshotTest.kt#ProposedIndicator
    @Composable
    private fun PullIndicator(fraction: Float, refreshing: Boolean, modifier: Modifier) {
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

    // The bar's round targets: a transparent Surface of 48 dp, what IconButton is (the guard refuses it in app code).
    @Composable
    private fun BarAction(icon: ImageVector, label: String) {
        Surface(onClick = {}, shape = CircleShape, color = Color.Transparent) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            }
        }
    }

    // FeedScreen's compactCount: 870, 12,345, 566K, 53.2M, exact while short, rounded once it would not fit.
    // https://github.com/213YaZ786/Yid/blob/main/app/src/main/java/com/yid/app/feature/feed/FeedScreen.kt#compactCount
    private fun compactCount(value: Long): String = when {
        value < 10_000 -> String.format(Locale.getDefault(), "%,d", value)
        value < 1_000_000 -> "${value / 1_000}K"
        else -> {
            val millions = value / 100_000 / 10.0
            String.format(Locale.getDefault(), "%.1fM", millions).replace(".0M", "M").replace(",0M", "M")
        }
    }
}

// What getProfile gives (BskyParser, core/model Feed): displayName, handle, description, createdAt as "Joined ...", the
// three counts, and the verification state (point 17).
private class HeaderContent(
    val name: String, val handle: String, val bio: String, val joined: String,
    val followers: Long, val following: Long, val posts: Long, val verified: Boolean
)

private val PersonContent = HeaderContent(
    "Ada Lindqvist", "ada.bsky.social",
    "Trail guide writer and map maker. Ridge walks on weekends, small-press zines the rest of the week. " +
        "Writing the guide one valley at a time.",
    "Joined March 2023", 1_204, 310, 4_521, verified = true
)

private val LongContent = HeaderContent(
    "Alexandria Montgomery-Featherstonehaugh of the Greater Northern Trail and Footpath Association",
    "alexandria-montgomery-featherstonehaugh.northern-trails.example.com",
    "Walking the same valley every week since 2011 and writing down what changes, from the first frost to the last " +
        "swallow. Volunteer path warden, occasional cartographer, full time believer in a good thermos. Opinions here are " +
        "my own and so are the boots. Open the map, bring water, tell someone where you are going, and leave the gates " +
        "as you found them. Replies are slow in summer, I am up a hill.",
    "Joined November 2022", 58_204, 1_893, 123_457, verified = true
)

// The banner behind (layout.ts PROFILE_BANNER_HEIGHT), the avatar at top 104 and left 10 inside its ring (Shell.tsx
// GrowableAvatar), the buttons row at the end below the banner (px_lg, pt_md), then the details at the start (px_lg).
// https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/ProfileHeaderStandard.tsx#ProfileHeaderStandard
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftHeader(following: Boolean, content: HeaderContent) {
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxWidth()) {
        // A stand-in for the banner picture: a vertical Brush of primary to tertiary, no Coil picture here.
        Box(
            Modifier.fillMaxWidth().height(ProfileBannerHeight)
                .semantics { contentDescription = "Banner" }
                .clickable(role = Role.Image, onClick = {})
                .background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                )
        )
        // Bluesky's StatusBarShadow: rgba(0,0,0,0.5) to transparent, the height of the inset.
        Box(
            Modifier.fillMaxWidth().height(inset).background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))
            )
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                // GrowableAvatar: top 104, left 10, width 94, size 90: a ring of the page's colour around the avatar.
                Box(
                    Modifier.padding(top = AvatarTop, start = AvatarLeft)
                        .size(AvatarSize + AvatarRing * 2)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                        .clip(CircleShape)
                        .semantics { contentDescription = "Profile picture" }
                        .clickable(role = Role.Image, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(url = null, name = content.name, size = AvatarSize)
                }
                // The buttons row: justify_end, px_lg 16, pt_md 12 below the banner.
                Row(
                    Modifier.align(Alignment.TopEnd).padding(top = ProfileBannerHeight + 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FollowButton(following = following, onClick = {})
                }
            }
            Spacer(Modifier.height(4.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DisplayName(content.name, content.verified)
                    // Handle.tsx: the handle in the contrast medium colour.
                    Text(
                        "@${content.handle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ProfileHeaderMetrics(content)
                // RichText numberOfLines 15: the bio as plain text here.
                Text(content.bio, style = MaterialTheme.typography.bodyMedium, maxLines = 15, overflow = TextOverflow.Ellipsis)
                ProfileItem(CalendarMonthRounded, "Joined", content.joined)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// DisplayName.tsx: the name bold, wrapping, the badge inline at its end (ProfileBadges inline, pl_xs).
// https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/DisplayName.tsx#ProfileHeaderDisplayName
@Composable
private fun DisplayName(name: String, verified: Boolean) {
    val badge = "verified"
    Text(
        buildAnnotatedString {
            append(name)
            if (verified) {
                append(" ")
                appendInlineContent(badge, "verified")
            }
        },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        inlineContent = mapOf(
            badge to InlineTextContent(Placeholder(22.sp, 22.sp, PlaceholderVerticalAlign.TextCenter)) {
                VerificationCheck(size = with(LocalDensity.current) { 20.sp.toDp() })
            }
        )
    )
}

// An icon (16 dp here) then the text; one wrapping line.
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/ProfileHeaderLinkedOutDraftScreenshotTest.kt#ProfileItem
@Composable
private fun ProfileItem(
    icon: ImageVector,
    description: String,
    text: String,
    textColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(4.dp))
        Text(
            text, style = MaterialTheme.typography.bodyMedium, color = textColor,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

// https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/ButtonMediumTokens.kt#ButtonMediumTokens
// https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/FilledButtonTokens.kt#FilledButtonTokens
// Follow as Material 3 Expressive's medium button with a leading icon, a clickable Row, no Material control, drawn as a
// glass zone (the app's rule): filled (primary, icon and label in onPrimary) or, once followed, glass only with the label
// and the check in the primary. HeaderStandardButtons: Plus and "Follow" in the primary colour, "Following" in the secondary.
// https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/ProfileHeaderStandard.tsx#HeaderStandardButtons
// not ported: ContainerShapeSquare and the Selected shapes (no toggle here), disabled and elevation tokens.
// not ported: textStyleFor (its one case here, titleMedium from the medium height up, is the Text's style below)
// not ported: the real FollowButton's animated container, line and content colours and AnimatedContent (a picture is
// one instant; the filled and followed states are two frames)
// https://github.com/213YaZ786/LinkedOut/blob/main/app/src/test/java/com/linkedout/app/screenshot/ProfileHeaderLinkedOutDraftScreenshotTest.kt#FollowButton
@Composable
private fun FollowButton(following: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val glass = LocalGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    val height = ButtonMediumContainerHeight
    // ContainerShapeRound (CornerFull) morphs to PressedContainerShape (CornerMedium = ShapeDefaults.Medium) with a spring.
    val full = with(density) { height.toPx() / 2 }
    val medium = ShapeDefaults.Medium.topStart.toPx(Size(full * 2, full * 2), density)
    val corner by animateFloatAsState(
        targetValue = if (pressed) medium else full,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "followCorner"
    )
    val shape = RoundedCornerShape(with(density) { corner.toDp() })
    val contentColor = if (following) colors.primary else colors.onPrimary
    GlassTarget(Modifier) {
        Row(
            Modifier
                .heightIn(min = height)
                .then(if (glass != null) Modifier.glassZone(shape, glass, lens = 1f) else Modifier)
                .clip(shape)
                .then(if (following) Modifier else Modifier.background(colors.primary, shape))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = contentColor),
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = ButtonMediumLeadingSpace),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (following) YidIcons.Check else PersonAddRounded,
                contentDescription = null,
                modifier = Modifier.size(ButtonMediumIconSize),
                tint = contentColor
            )
            Spacer(Modifier.width(ButtonMediumIconLabelSpace))
            // textStyleFor: titleMedium from the medium height up.
            Text(if (following) "Following" else "Follow", style = MaterialTheme.typography.titleMedium, color = contentColor)
        }
    }
}

// ButtonMediumTokens
private val ButtonMediumContainerHeight: Dp = 56.dp
private val ButtonMediumIconLabelSpace: Dp = 8.dp
private val ButtonMediumIconSize: Dp = 24.dp
private val ButtonMediumLeadingSpace: Dp = 24.dp

// Metrics.tsx: followers, following and posts on one line, a count in semi bold then its word in the contrast medium
// colour; a flow so 200 % wraps.
// https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/Metrics.tsx#ProfileHeaderMetrics
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeaderMetrics(content: HeaderContent) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MetricsItem(count(content.followers), "followers")
        MetricsItem(count(content.following), "following")
        MetricsItem(count(content.posts), "posts")
    }
}

// The count bold, its word in the contrast medium colour (Metrics.tsx's two Texts).
// https://github.com/bluesky-social/social-app/blob/main/src/screens/Profile/Header/Metrics.tsx#ProfileHeaderMetrics
@Composable
private fun MetricsItem(primaryText: String, secondaryText: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$primaryText ") }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(secondaryText) }
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

// formatCount of Metrics.tsx, as Yiḍ's FeedScreen writes counts: exact while short, rounded once it would not fit.
private fun count(value: Long): String = when {
    value < 10_000 -> String.format(Locale.getDefault(), "%,d", value)
    value < 1_000_000 -> "${value / 1_000}K"
    else -> "${value / 100_000 / 10.0}M"
}

// layout.ts PROFILE_BANNER_HEIGHT, Shell.tsx GrowableAvatar (top 104, left 10, width 94, size 90)
private val ProfileBannerHeight: Dp = 150.dp
private val AvatarSize: Dp = 90.dp
private val AvatarRing: Dp = 2.dp
private val AvatarTop: Dp = 104.dp
private val AvatarLeft: Dp = 10.dp

private fun symbol(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, viewportWidth = 960f, viewportHeight = 960f)
        .addPath(pathData = PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.White))
        .build()

// Material Symbols Rounded "person_add".
// https://github.com/google/material-design-icons/blob/master/symbols/android/person_add/materialsymbolsrounded/person_add_24px.xml
private val PersonAddRounded: ImageVector by lazy {
    symbol(
        "PersonAdd",
        "M720,440L640,440Q623,440 611.5,428.5Q600,417 600,400Q600,383 611.5,371.5Q623,360 640,360L720,360L720,280" +
            "Q720,263 731.5,251.5Q743,240 760,240Q777,240 788.5,251.5Q800,263 800,280L800,360L880,360Q897,360 908.5,371.5" +
            "Q920,383 920,400Q920,417 908.5,428.5Q897,440 880,440L800,440L800,520Q800,537 788.5,548.5Q777,560 760,560" +
            "Q743,560 731.5,548.5Q720,537 720,520L720,440ZM360,480Q294,480 247,433Q200,386 200,320Q200,254 247,207" +
            "Q294,160 360,160Q426,160 473,207Q520,254 520,320Q520,386 473,433Q426,480 360,480ZM40,720L40,688" +
            "Q40,654 57.5,625.5Q75,597 104,582Q166,551 230,535.5Q294,520 360,520Q426,520 490,535.5Q554,551 616,582" +
            "Q645,597 662.5,625.5Q680,654 680,688L680,720Q680,753 656.5,776.5Q633,800 600,800L120,800Q87,800 63.5,776.5" +
            "Q40,753 40,720ZM120,720L600,720L600,688Q600,677 594.5,668Q589,659 580,654Q526,627 471,613.5Q416,600 360,600" +
            "Q304,600 249,613.5Q194,627 140,654Q131,659 125.5,668Q120,677 120,688L120,720ZM360,400Q393,400 416.5,376.5" +
            "Q440,353 440,320Q440,287 416.5,263.5Q393,240 360,240Q327,240 303.5,263.5Q280,287 280,320Q280,353 303.5,376.5" +
            "Q327,400 360,400Z"
    )
}

// Material Symbols Rounded "calendar_month".
// https://github.com/google/material-design-icons/blob/master/symbols/android/calendar_month/materialsymbolsrounded/calendar_month_24px.xml
private val CalendarMonthRounded: ImageVector by lazy {
    symbol(
        "CalendarMonth",
        "M200,880Q167,880 143.5,856.5Q120,833 120,800L120,240Q120,207 143.5,183.5Q167,160 200,160L240,160L240,120" +
            "Q240,103 251.5,91.5Q263,80 280,80Q297,80 308.5,91.5Q320,103 320,120L320,160L640,160L640,120Q640,103 651.5,91.5" +
            "Q663,80 680,80Q697,80 708.5,91.5Q720,103 720,120L720,160L760,160Q793,160 816.5,183.5Q840,207 840,240L840,800" +
            "Q840,833 816.5,856.5Q793,880 760,880L200,880ZM200,800L760,800L760,400L200,400L200,800ZM200,320L760,320L760,240" +
            "L200,240L200,320ZM480,560Q463,560 451.5,548.5Q440,537 440,520Q440,503 451.5,491.5Q463,480 480,480" +
            "Q497,480 508.5,491.5Q520,503 520,520Q520,537 508.5,548.5Q497,560 480,560ZM320,560Q303,560 291.5,548.5" +
            "Q280,537 280,520Q280,503 291.5,491.5Q303,480 320,480Q337,480 348.5,491.5Q360,503 360,520Q360,537 348.5,548.5" +
            "Q337,560 320,560ZM640,560Q623,560 611.5,548.5Q600,537 600,520Q600,503 611.5,491.5Q623,480 640,480" +
            "Q657,480 668.5,491.5Q680,503 680,520Q680,537 668.5,548.5Q657,560 640,560ZM480,720Q463,720 451.5,708.5" +
            "Q440,697 440,680Q440,663 451.5,651.5Q463,640 480,640Q497,640 508.5,651.5Q520,663 520,680Q520,697 508.5,708.5" +
            "Q497,720 480,720ZM320,720Q303,720 291.5,708.5Q280,697 280,680Q280,663 291.5,651.5Q303,640 320,640" +
            "Q337,640 348.5,651.5Q360,663 360,680Q360,697 348.5,708.5Q337,720 320,720ZM640,720Q623,720 611.5,708.5" +
            "Q600,697 600,680Q600,663 611.5,651.5Q623,640 640,640Q657,640 668.5,651.5Q680,663 680,680Q680,697 668.5,708.5" +
            "Q657,720 640,720Z"
    )
}
